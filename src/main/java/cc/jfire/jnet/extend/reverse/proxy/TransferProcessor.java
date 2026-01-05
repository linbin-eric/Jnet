package cc.jfire.jnet.extend.reverse.proxy;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;

import java.util.Comparator;
import java.util.List;

public class TransferProcessor implements ReadProcessor<HttpRequestPart>
{
    private final ResourceHandler[] handlers;
    private ResourceHandler currentHandler;

    public TransferProcessor(List<ResourceConfig> configs, HttpConnection2Pool pool)
    {
        handlers = configs.stream()
                         .sorted(Comparator.comparingInt(ResourceConfig::getOrder))
                         .map(config -> config.parse(pool))
                         .toArray(ResourceHandler[]::new);
    }

    @Override
    public void read(HttpRequestPart part, ReadProcessorNode next)
    {
        if (part instanceof HttpRequestPartHead head)
        {
            processHead(head, next);
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
            processBody(part, next);
        }
        else if (part instanceof HttpRequestPartEnd end)
        {
            processEnd(end, next);
        }
    }

    private void processHead(HttpRequestPartHead head, ReadProcessorNode next)
    {
        for (ResourceHandler handler : handlers)
        {
            if (handler.match(head))
            {
                currentHandler = handler;
                handler.process(head, next.pipeline());
                return;
            }
        }
        // 没有匹配的 handler
        String path = head.getPath();
        head.close();
        FullHttpResp response = new FullHttpResp();
        response.getHead().setResponseCode(404);
        response.getBody().setBodyText("not found address:" + path);
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

    private void processEnd(HttpRequestPartEnd end, ReadProcessorNode next)
    {
        if (currentHandler != null)
        {
            currentHandler.process(end, next.pipeline());
            currentHandler = null;
        }
        else
        {
            end.close();
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
        }
        next.fireReadFailed(e);
    }
}

