package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.DataIgnore;
import cc.jfire.jnet.common.util.HttpCoderUtil;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpResponseChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpResponseFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;

import java.nio.charset.StandardCharsets;

public class HttpRespEncoder implements WriteProcessor<Object>
{
    public static final byte[]          NEWLINE = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private final       BufferAllocator allocator;

    public HttpRespEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    /**
     * 编码 HttpResponsePartHead 到 IoBuffer
     */
    private IoBuffer encodeHead(HttpResponsePartHead head)
    {
        IoBuffer buffer       = allocator.allocate(1024);
        String   responseLine = STR.format("{} {} {}\r\n", head.getVersion(), head.getStatusCode(), head.getReasonPhrase() != null ? head.getReasonPhrase() : "");
        buffer.put(responseLine.getBytes(StandardCharsets.US_ASCII));
        HttpCoderUtil.writeHeaderValue(head.getHeaders(), buffer);
        buffer.put(NEWLINE);
        return buffer;
    }

    @Override
    public void write(Object obj, WriteProcessorNode next)
    {
        switch (obj)
        {
            case HttpResponse httpResponse ->
            {
                // 先设置必要的头部信息
                httpResponse.helpSetContentLengthIfNeed();
                httpResponse.helpSetContentTypeIfNeed();
                // 编码 head 部分
                IoBuffer headBuffer = encodeHead(httpResponse.getHead());
                next.fireWrite(headBuffer);
                // 再编码 body 部分
                IoBuffer bodyBuffer = allocator.allocate(1024);
                httpResponse.writeBody(bodyBuffer);
                next.fireWrite(bodyBuffer);
            }
            case HttpResponsePartHead head ->
            {
                // 如果 part 不为空，直接写出原始 buffer
                IoBuffer part = head.getPart();
                if (part != null)
                {
                    next.fireWrite(part);
                }
                else
                {
                    // part 为空，基于属性重新编码
                    IoBuffer buffer = encodeHead(head);
                    next.fireWrite(buffer);
                }
            }
            case IoBuffer ignored -> next.fireWrite(obj);
            case HttpResponseChunkedBodyPart chunkedBodyPart ->
            {
                IoBuffer part = chunkedBodyPart.getPart();
                if (part != null)
                {
                    next.fireWrite(part);
                }
            }
            case HttpResponseFixLengthBodyPart fixLengthBodyPart ->
            {
                IoBuffer part = fixLengthBodyPart.getPart();
                if (part != null)
                {
                    next.fireWrite(part);
                }
            }
            case DataIgnore ignored -> next.fireWrite(obj);
            default -> throw new IllegalArgumentException(STR.format("HttpRespPartEncoder不支持入参类型:{}", obj.getClass()));
        }
    }
}
