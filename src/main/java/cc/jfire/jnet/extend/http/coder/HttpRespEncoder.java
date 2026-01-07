package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.DataIgnore;
import cc.jfire.jnet.extend.http.dto.FullHttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRespEncoder implements WriteProcessor<Object>
{
    private final       BufferAllocator allocator;
    public static final byte[]          NEWLINE          = "\r\n".getBytes(StandardCharsets.US_ASCII);

    public HttpRespEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public void write(Object obj, WriteProcessorNode next)
    {
        if (obj instanceof FullHttpResponse fullHttpResponse)
        {
            IoBuffer buffer = allocator.allocate(1024);
            fullHttpResponse.write(buffer);
            next.fireWrite(buffer);
        }
        else if (obj instanceof HttpResponsePartHead head)
        {
//            log.trace("[HttpRespEncoder] 编码HttpResponsePartHead, statusCode: {}", head.getStatusCode());
            // 如果 part 不为空，直接写出原始 buffer
            IoBuffer part = head.getPart();
            if (part != null)
            {
//                log.trace("[HttpRespEncoder] 使用原始part buffer, 大小: {}", part.remainRead());
                next.fireWrite(part);
            }
            else
            {
                // part 为空，基于属性重新编码
//                log.trace("[HttpRespEncoder] 重新编码响应头");
                IoBuffer buffer = allocator.allocate(1024);
                // 编码响应行：HTTP/1.1 200 OK\r\n
                String responseLine = head.getVersion() + " " + head.getStatusCode() + " " +
                                      (head.getReasonPhrase() != null ? head.getReasonPhrase() : "") + "\r\n";
                buffer.put(responseLine.getBytes(StandardCharsets.US_ASCII));
                // 编码 headers
                head.getHeaders().forEach((name, value) -> {
                    buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII));
                });
                // 空行结束头部
                buffer.put(NEWLINE);
//                log.trace("[HttpRespEncoder] 响应头编码完成, buffer大小: {}", buffer.remainRead());
                next.fireWrite(buffer);
            }
        }
        else if (obj instanceof IoBuffer ioBuffer)
        {
//            log.trace("[HttpRespEncoder] 透传IoBuffer, 大小: {}", ioBuffer.remainRead());
            next.fireWrite(obj);
        }
        else if (obj instanceof DataIgnore)
        {
//            log.trace("[HttpRespEncoder] 透传DataIgnore");
            next.fireWrite(obj);
        }
        else
        {
//            log.error("[HttpRespEncoder] 不支持的类型: {}", obj.getClass());
            throw new IllegalArgumentException(STR.format("HttpRespPartEncoder不支持入参类型:{}", obj.getClass()));
        }
    }
}
