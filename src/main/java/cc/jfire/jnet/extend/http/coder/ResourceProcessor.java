package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.extend.reverse.proxy.ContentTypeDist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResourceProcessor implements ReadProcessor<Object>
{
    private final String                                prefixPath;
    private final boolean                               runInIDE;
    private final ConcurrentMap<String, StaticResource> map = new ConcurrentHashMap<>();
    public ResourceProcessor(String prefixPath, boolean runInIDE)
    {
        this.prefixPath = prefixPath;
        this.runInIDE   = runInIDE;
    }

    public void setNotFound(String url, HttpRequest httpRequest)
    {
        String purePath = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        map.put(purePath, new StaticResource(("unAvailable Path:" + purePath).getBytes(StandardCharsets.UTF_8), "text/html"));
    }

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        // 只处理 HttpRequest 类型，其他类型直接透传
        if (!(data instanceof HttpRequest httpRequest))
        {
            next.fireRead(data);
            return;
        }
        handleHttpRequest(httpRequest, next);
    }

    private void handleHttpRequest(HttpRequest httpRequest, ReadProcessorNode next)
    {
        String url = httpRequest.getHead().getPath();
        if (!httpRequest.getHead().getMethod().equalsIgnoreCase("get"))
        {
            next.fireRead(httpRequest);
        }
        else
        {
            if (url.equalsIgnoreCase("/"))
            {
                url = "/index.html";
            }
            String purePath = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            if (runInIDE)
            {
                String contentType;
                int    i = purePath.lastIndexOf(".");
                if (i == -1)
                {
                    contentType = "text/html";
                }
                else
                {
                    String suffix = purePath.substring(i);
                    String s      = ContentTypeDist.get(suffix);
                    contentType = s == null ? "text/html" : s;
                }
                String realClassResourcePath = prefixPath + purePath;
//                log.trace("当前请求路径为:{}", realClassResourcePath);
                StaticResource staticResource = null;
                try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(realClassResourcePath))
                {
                    if (resourceAsStream != null)
                    {
                        staticResource = new StaticResource(IoUtil.readAllBytes(resourceAsStream), contentType);
                    }
                    else
                    {
                        ;
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                if (staticResource == null)
                {
                    next.fireRead(httpRequest);
                }
                else
                {
                    httpRequest.close();
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", staticResource.contentType);
                    response.setBodyBytes(staticResource.content);
                    response.addHeader("Cache-Control", "no-cache");
                    response.addHeader("Connection", "keep-alive");
                    next.pipeline().fireWrite(response);
                }
            }
            else
            {
                StaticResource staticResource = map.computeIfAbsent(purePath, str -> {
                    String contentType;
                    int    i = str.lastIndexOf(".");
                    if (i == -1)
                    {
                        contentType = "text/html";
                    }
                    else
                    {
                        String suffix = str.substring(i);
                        String s      = ContentTypeDist.get(suffix);
                        contentType = s == null ? "text/html" : s;
                    }
                    String realClassResourcePath = prefixPath + str;
//                    log.debug("当前请求路径为:{}", realClassResourcePath);
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
                    next.fireRead(httpRequest);
                }
                else
                {
                    httpRequest.close();
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", staticResource.contentType);
                    response.setBodyBytes(staticResource.content);
                    response.addHeader("Cache-Control", "max-age=3600");
                    next.pipeline().fireWrite(response);
                }
            }
        }
    }

    record StaticResource(byte[] content, String contentType)
    {
    }
}
