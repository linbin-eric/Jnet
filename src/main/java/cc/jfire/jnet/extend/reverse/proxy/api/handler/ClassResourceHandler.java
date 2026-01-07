package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.FullHttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassResourceHandler extends AbstractIOResourceHandler
{
    record Tuper(String contentType, byte[] bytes)
    {
    }

    public ClassResourceHandler(String matchUrl, String originPath)
    {
        super(matchUrl, originPath);
    }

    private ConcurrentHashMap<String, Tuper> map = new ConcurrentHashMap<>();

    @Override
    protected void processHead(HttpRequestPartHead head, Pipeline pipeline, String requestUrl, String contentType)
    {
        String realClassResourcePath = path + requestUrl;
        Tuper tuper = map.computeIfAbsent(realClassResourcePath, url -> {
            try (InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(realClassResourcePath))
            {
                if (resourceAsStream != null)
                {
                    byte[] bytes = IoUtil.readAllBytes(resourceAsStream);
                    return new Tuper(contentType, bytes);
                }
                else
                {
                    return new Tuper("text/html;charset=utf-8", STR.format("not available path:{},not find in :{}", head.getPath(), realClassResourcePath).getBytes(StandardCharsets.UTF_8));
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
        head.close();
        FullHttpResponse response = new FullHttpResponse();
        response.addHeader("Content-Type", contentType);
        response.setBodyBytes(tuper.bytes());
        pipeline.fireWrite(response);
    }
}
