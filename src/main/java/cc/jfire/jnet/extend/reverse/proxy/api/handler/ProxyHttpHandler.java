package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpReceiveResponse;
import cc.jfire.jnet.extend.http.client.HttpSendRequest;
import cc.jfire.jnet.extend.http.client.Part;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;

public sealed abstract class ProxyHttpHandler implements ResourceHandler permits PrefixMatchProxyHttpHandler, FullMatchProxyHttpHandler
{
    protected void proxyBackendUrl(HttpRequest request, Pipeline pipeline, String backendUrl)
    {
        HttpSendRequest httpSendRequest = new HttpSendRequest();
        httpSendRequest.setUrl(backendUrl).setMethod(request.getMethod());
        request.getHeaders().forEach((name, value) -> httpSendRequest.putHeader(name, value));
        if (request.getBody() == null)
        {
            ;
        }
        else
        {
            httpSendRequest.setBody(request.getBody().slice(request.getBody().remainRead()));
        }
        request.close();
        try (HttpReceiveResponse httpReceiveResponse = HttpClient.newCall(httpSendRequest))
        {
            httpReceiveResponse.waitForReceiveFinish(1000 * 60 * 5);
            IoBuffer buffer = pipeline.allocator().allocate(httpReceiveResponse.getContentLength() > 0 ? httpReceiveResponse.getContentLength() : 1024);
            Part     part;
            while ((part = httpReceiveResponse.pollChunk(1000 * 60)) != null && !part.endOfBody())
            {
                buffer.put(part.originData());
                part.free();
            }
            FullHttpResp httpResponse = new FullHttpResp();
            httpResponse.getHead().setResponseCode(httpReceiveResponse.getHttpCode());
            if (buffer.remainRead() > 0)
            {
                httpResponse.getBody().setBodyBuffer(buffer);
            }
            httpReceiveResponse.getHeaders().forEach((name, value) -> httpResponse.getHead().addHeader(name, value));
            pipeline.fireWrite(httpResponse);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
