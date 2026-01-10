package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.RuntimeJVM;
import cc.jfire.baseutil.STR;
import cc.jfire.baseutil.StringUtil;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.HttpCoderUtil;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class AbstractResourceEncoder implements ReadProcessor<HttpRequest>
{
    //资源地址的文件夹路径
    protected final String resourcePathPrefix;
    /**
     * 请求的url的前缀
     * 比如请求url的前缀是/dist，资源地址的文件夹路径是a，那么一个请求路径为/dist/index.html的文件应该在实际的a/index.html来寻找。
     */
    protected final String urlPrefix;
    protected final int    urlPrefixLength;

    public AbstractResourceEncoder(String resourcePathPrefix, String urlPrefix)
    {
        this.resourcePathPrefix = resourcePathPrefix;
        this.urlPrefix          = urlPrefix;
        urlPrefixLength         = urlPrefix.length();
    }

    public static AbstractResourceEncoder from(String resource, String urlPrefix)
    {
        if (resource.startsWith("classpath:"))
        {
            return new ClassResourceEncoder(resource.substring(10), urlPrefix);
        }
        else if (resource.startsWith("file:"))
        {
            return new FileResourceEncoder(resource.substring(5), urlPrefix);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void read(HttpRequest request, ReadProcessorNode next)
    {
        String url = request.getHead().getPath();
        if (!request.getHead().getMethod().equalsIgnoreCase("get"))
        {
            next.fireRead(request);
        }
        else
        {
            if (urlPrefixLength != 0)
            {
                if (!url.startsWith(urlPrefix))
                {
                    next.fireRead(request);
                    return;
                }
                else
                {
                    url = url.substring(urlPrefixLength);
                }
            }
            url = HttpCoderUtil.pureUrl(url);
            if (StringUtil.isBlank(url))
            {
                url = "index.html";
            }
            else if (url.equals("/"))
            {
                url = "/index.html";
            }
            process(request, url, next);
        }
    }

    protected abstract void process(HttpRequest request, String url, ReadProcessorNode next);

    public static class ClassResourceEncoder extends AbstractResourceEncoder
    {
        record StaticResource(byte[] content, String contentType)
        {
        }

        private ConcurrentMap<String, StaticResource> map = new ConcurrentHashMap<>();

        public ClassResourceEncoder(String resourcePathPrefix, String urlPrefix)
        {
            super(resourcePathPrefix, urlPrefix);
        }

        @Override
        protected void process(HttpRequest request, String url, ReadProcessorNode next)
        {
            StaticResource staticResource = map.computeIfAbsent(url, str -> {
                String contentType           = HttpCoderUtil.findContentType(str);
                String realClassResourcePath = resourcePathPrefix + str;
//                log.trace("当前请求路径为:{}", realClassResourcePath);
                try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(realClassResourcePath))
                {
                    if (resourceAsStream != null)
                    {
                        return new StaticResource(IoUtil.readAllBytes(resourceAsStream), contentType);
                    }
                    else
                    {
                        return null;
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });
            if (staticResource == null)
            {
                next.fireRead(request);
            }
            else
            {
                request.close();
                HttpResponse response = new HttpResponse();
                response.addHeader("Content-Type", staticResource.contentType());
                response.setBodyBytes(staticResource.content());
                response.addHeader("Cache-Control", "max-age=3600");
                next.pipeline().fireWrite(response);
            }
        }
    }

    public static class FileResourceEncoder extends AbstractResourceEncoder
    {
        protected final File dir;

        public FileResourceEncoder(String resourcePathPrefix, String urlPrefix)
        {
            super(resourcePathPrefix, urlPrefix);
            File dirOfMainClass = RuntimeJVM.getDirOfMainClass();
            dir = new File(dirOfMainClass, resourcePathPrefix);
            if (dir.exists() == false || dir.isDirectory() == false)
            {
                throw new IllegalArgumentException(STR.format("配置的路径:{}不存在或不是文件夹", dir.getAbsolutePath()));
            }
        }

        @Override
        protected void process(HttpRequest request, String url, ReadProcessorNode next)
        {
            File target = new File(dir, url);
            if (!target.exists())
            {
                next.fireRead(request);
            }
            else
            {
                request.close();
                String contentType = HttpCoderUtil.findContentType(url);
                try (FileInputStream inputStream = new FileInputStream(target))
                {
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", contentType);
                    response.setBodyBytes(IoUtil.readAllBytes(inputStream));
                    next.pipeline().fireWrite(response);
                }
                catch (IOException e)
                {
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", "text");
                    response.setBodyText("error:" + e.getMessage());
                    next.pipeline().fireWrite(response);
                }
            }
        }
    }
}
