package com.jfirer.jnet.extend.reverse.proxy;

import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.extend.http.dto.FullHttpResp;
import com.jfirer.jnet.extend.http.dto.HttpRequest;
import com.jfirer.jnet.extend.reverse.proxy.api.ResourceConfig;
import com.jfirer.jnet.extend.reverse.proxy.api.ResourceHandler;

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
        response.getBody().setBodyText("not found address:" + request.getUrl());
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
