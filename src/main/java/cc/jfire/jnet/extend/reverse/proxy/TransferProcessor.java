package cc.jfire.jnet.extend.reverse.proxy;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class TransferProcessor implements ReadProcessor<HttpRequestPart>
{
    private final ResourceHandler[] handlers;
    private       ResourceHandler   currentHandler;

    public TransferProcessor(List<ResourceConfig> configs, HttpConnectionPool pool)
    {
        handlers = configs.stream().sorted(Comparator.comparingInt(ResourceConfig::getOrder)).map(config -> config.parse(pool)).toArray(ResourceHandler[]::new);
    }

    @Override
    public void read(HttpRequestPart part, ReadProcessorNode next)
    {
//        log.trace("[TransferProcessor] 收到请求部分: {}, isLast: {}", part.getClass().getSimpleName(), part.isLast());
        if (part instanceof HttpRequestPartHead head)
        {
//            log.trace("[TransferProcessor] 处理请求头: {} {}", head.getMethod(), head.getPath());
            processHead(head, next);
            // 如果是无 body 请求，清除 currentHandler
            if (head.isLast())
            {
//                log.trace("[TransferProcessor] 无body请求完成, 清除currentHandler");
                currentHandler = null;
            }
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
//            log.trace("[TransferProcessor] 处理请求体: {}", part.getClass().getSimpleName());
            processBody(part, next);
            // 如果是最后一个 body，清除 currentHandler
            if (part.isLast())
            {
//                log.trace("[TransferProcessor] 请求体完成(last=true), 清除currentHandler");
                currentHandler = null;
            }
        }
    }

    private void processHead(HttpRequestPartHead head, ReadProcessorNode next)
    {
        for (ResourceHandler handler : handlers)
        {
            // process 方法返回 true 表示已处理，返回 false 表示不匹配
            if (handler.process(head, next.pipeline()))
            {
//                log.trace("[TransferProcessor] 匹配到handler: {}", handler.getClass().getSimpleName());
                currentHandler = handler;
                return;
            }
        }
        // 没有匹配的 handler
        String path = head.getPath();
//        log.warn("[TransferProcessor] 没有匹配的handler, 返回404: {}", path);
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
//            log.trace("[TransferProcessor] 转发请求体到handler: {}", currentHandler.getClass().getSimpleName());
            currentHandler.process(body, next.pipeline());
        }
        else
        {
//            log.warn("[TransferProcessor] 无currentHandler, 丢弃body");
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
        }
        next.fireReadFailed(e);
    }
}

