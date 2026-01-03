package cc.jfire.jnet.extend.reverse.proxy;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;

import java.util.Comparator;
import java.util.List;

public class TransferProcessor implements ReadProcessor<HttpRequest>
{
    private ResourceHandler[] handlers;

    public TransferProcessor(List<ResourceConfig> configs)
    {
        handlers = configs.stream()//
                         .sorted(Comparator.comparingInt(ResourceConfig::getOrder))//
                         .map(ResourceConfig::parse).toArray(ResourceHandler[]::new);
    }

    @Override
    public void read(HttpRequest request, ReadProcessorNode next)
    {
        for (ResourceHandler handler : handlers)
        {
            if (handler.process(request, next.pipeline()))
            {
                return;
            }
        }
        request.close();
        FullHttpResp response = new FullHttpResp();
        response.getBody().setBodyText("not found address:" + request.getPath());
        next.pipeline().fireWrite(response);
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
            ;
        }
        next.fireReadFailed(e);
    }
}
