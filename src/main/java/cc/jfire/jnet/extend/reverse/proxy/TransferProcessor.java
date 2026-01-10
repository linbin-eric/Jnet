package cc.jfire.jnet.extend.reverse.proxy;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class TransferProcessor implements ReadProcessor<Object>
{
    private final ResourceHandler[] handlers;
    private       ResourceHandler   currentHandler;
    private       boolean           webSocketMode = false;

    public TransferProcessor(List<ResourceConfig> configs, HttpConnectionPool pool)
    {
        handlers = configs.stream().sorted(Comparator.comparingInt(ResourceConfig::getOrder)).map(config -> config.parse(pool)).toArray(ResourceHandler[]::new);
    }

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        if (data instanceof IoBuffer buffer)
        {
            processWebSocketData(buffer, next);
        }
        else if (data instanceof HttpRequestPart part)
        {
            processHttpRequestPart(part, next);
        }
    }

    private void processWebSocketData(IoBuffer buffer, ReadProcessorNode next)
    {
        if (currentHandler != null)
        {
            currentHandler.processWebSocket(buffer, next.pipeline());
        }
        else
        {
            buffer.free();
        }
    }

    private void processHttpRequestPart(HttpRequestPart part, ReadProcessorNode next)
    {
        if (part instanceof HttpRequestPartHead head)
        {
            processHead(head, next);
            // WebSocket 模式下不清除 currentHandler
            if (head.isLast() && !webSocketMode)
            {
                currentHandler = null;
            }
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
            processBody(part, next);
            // WebSocket 模式下不清除 currentHandler
            if (part.isLast() && !webSocketMode)
            {
                currentHandler = null;
            }
        }
    }

    private void processHead(HttpRequestPartHead head, ReadProcessorNode next)
    {
        for (ResourceHandler handler : handlers)
        {
            if (handler.process(head, next.pipeline()))
            {
                currentHandler = handler;
                // 检查是否是 WebSocket 握手请求
                if (head.isWebSocketUpgrade())
                {
                    webSocketMode = true;
                }
                return;
            }
        }
        // 没有匹配的 handler
        String path = head.getPath();
        head.close();
        HttpResponse response = new HttpResponse();
        response.getHead().setStatusCode(404);
        response.getHead().setReasonPhrase("Not Found");
        response.setBodyText("not found address:" + path);
        next.pipeline().fireWrite(response);
    }

    private void processBody(HttpRequestPart body, ReadProcessorNode next)
    {
        if (currentHandler != null)
        {
            currentHandler.process(body, next.pipeline());
        }
        else
        {
            body.close();
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        try
        {
            for (ResourceHandler handler : handlers)
            {
                handler.readFailed(e);
            }
        }
        finally
        {
            currentHandler = null;
            webSocketMode = false;
        }
        next.fireReadFailed(e);
    }
}
