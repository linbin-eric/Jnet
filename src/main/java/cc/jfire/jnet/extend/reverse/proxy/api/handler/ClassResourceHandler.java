package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRequest;

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
    protected void process(HttpRequest httpRequest, Pipeline pipeline, String requestUrl, String contentType)
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
                    return new Tuper("text/html;charset=utf-8", STR.format("not available path:{},not find in :{}", httpRequest.getUrl(), realClassResourcePath).getBytes(StandardCharsets.UTF_8));
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
        httpRequest.close();
        FullHttpResp response = new FullHttpResp();
        response.getHead().addHeader("Content-Type",contentType);
        response.getBody().setBodyBytes(tuper.bytes());
        pipeline.fireWrite(response);
    }
}
