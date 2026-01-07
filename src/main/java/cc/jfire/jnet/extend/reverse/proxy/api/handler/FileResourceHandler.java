package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.RuntimeJVM;
import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.FullHttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public final class FileResourceHandler extends AbstractIOResourceHandler
{
    private File dir;

    public FileResourceHandler(String prefixMatch, String originPath)
    {
        super(prefixMatch, originPath);
        if (isAbsolutePath(path))
        {
            dir = new File(path);
        }
        else
        {
            File tmp = RuntimeJVM.getDirOfMainClass();
            while (path.startsWith("../"))
            {
                tmp  = tmp.getParentFile();
                path = path.substring(3);
            }
            dir = new File(tmp, path);
        }
        if (!dir.isDirectory())
        {
            throw new IllegalArgumentException(STR.format("路径:{}应该是一个文件夹，而不是文件", originPath));
        }
    }

    @Override
    protected void processHead(HttpRequestPartHead head, Pipeline pipeline, String requestUrl, String contentType)
    {
        File resourceFile = new File(dir, requestUrl);
        if (resourceFile.exists())
        {
            try (InputStream inputStream = new FileInputStream(resourceFile))
            {
                byte[] bytes = IoUtil.readAllBytes(inputStream);
                head.close();
                FullHttpResponse response = new FullHttpResponse();
                response.addHeader("Content-Type", contentType);
                response.setBodyBytes(bytes);
                pipeline.fireWrite(response);
            }
            catch (IOException e)
            {
                throw new RuntimeException("读取文件地址:" + resourceFile.getAbsolutePath() + "出现异常", e);
            }
        }
        else
        {
            head.close();
            FullHttpResponse response = new FullHttpResponse();
            response.addHeader("Content-Type", "text/html;charset=utf-8");
            response.setBodyText(STR.format("not available path:{},not find in :{}", requestUrl, resourceFile.getAbsolutePath()));
            pipeline.fireWrite(response);
        }
    }

    private static boolean isAbsolutePath(String path)
    {
        if (path.length() == 0)
        {
            return false;
        }
        char c = path.charAt(0);
        //这个地址是绝对路径
        return c == '/' || (c >= 'a' && c <= 'z' && path.charAt(1) == ':') || (c >= 'A' && c <= 'Z' && path.charAt(1) == ':');
    }
}
