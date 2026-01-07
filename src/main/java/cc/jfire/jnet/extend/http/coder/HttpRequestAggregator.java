package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpRequestChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

public class HttpRequestAggregator implements ReadProcessor<HttpRequestPart>
{
    private HttpRequestPartHead head;
    private IoBuffer            body;

    @Override
    public void read(HttpRequestPart data, ReadProcessorNode next)
    {
        if (data instanceof HttpRequestPartHead)
        {
            head = (HttpRequestPartHead) data;
            // 如果是无 body 请求，直接聚合并发送
            if (head.isLast())
            {
                fireAggregatedRequest(next);
            }
        }
        else if (data instanceof HttpRequestFixLengthBodyPart part)
        {
            if (body == null)
            {
                body = part.getPart();
            }
            else
            {
                body.put(part.getPart());
                part.getPart().free();
            }
            // 如果是最后一个 body，聚合并发送
            if (part.isLast())
            {
                fireAggregatedRequest(next);
            }
        }
        else if (data instanceof HttpRequestChunkedBodyPart chunkedPart)
        {
            IoBuffer                   chunkBuffer = chunkedPart.getPart();
            int                        dataLength  = chunkedPart.getChunkLength() - chunkedPart.getHeadLength() - 2;
            chunkBuffer.setReadPosi(chunkedPart.getHeadLength());
            IoBuffer dataBuffer = chunkBuffer.slice(dataLength);
            chunkBuffer.free();
            if (body == null)
            {
                body = dataBuffer;
            }
            else
            {
                body.put(dataBuffer);
                dataBuffer.free();
            }
            // 如果是最后一个 chunk，聚合并发送
            if (chunkedPart.isLast())
            {
                fireAggregatedRequest(next);
            }
        }
    }

    /**
     * 聚合请求并发送到下游
     */
    private void fireAggregatedRequest(ReadProcessorNode next)
    {
        HttpRequest request = new HttpRequest();
        request.setHead(head);
        if (head.isChunked())
        {
            head.setContentLength(body == null ? 0 : body.remainRead());
        }
        request.setBody(body);
        next.fireRead(request);
        head = null;
        body = null;
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (body != null)
        {
            body.free();
            body = null;
        }
        if (head != null)
        {
            head.close();
            head = null;
        }
        next.fireReadFailed(e);
    }
}
