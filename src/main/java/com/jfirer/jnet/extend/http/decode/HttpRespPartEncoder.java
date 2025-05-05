package com.jfirer.jnet.extend.http.decode;

import com.jfirer.baseutil.STR;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ReflectUtil;
import com.jfirer.jnet.extend.http.dto.FullHttpResp;
import com.jfirer.jnet.extend.http.dto.HttpRespBody;
import com.jfirer.jnet.extend.http.dto.HttpRespHead;

import java.nio.charset.StandardCharsets;

public class HttpRespPartEncoder implements WriteProcessor<Object>
{
    private final       BufferAllocator allocator;
    public static final byte[]          NEWLINE = "\r\n".getBytes(StandardCharsets.US_ASCII);

    public HttpRespPartEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public void write(Object obj, WriteProcessorNode next)
    {
        if (obj instanceof HttpRespHead head)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            putHead(head, buffer);
            next.fireWrite(buffer);
        }
        else if (obj instanceof HttpRespBody body)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            putBody(body, buffer);
            next.fireWrite(buffer);
        }
        else if (obj instanceof FullHttpResp fullHttpResp)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            try
            {
                putHead(fullHttpResp.getHead(), buffer);
                if (fullHttpResp.getBody().isEmpty() == false)
                {
                    putBody(fullHttpResp.getBody(), buffer);
                }
                next.fireWrite(buffer);
            }
            catch (Throwable e)
            {
                ReflectUtil.throwException(e);
            }
        }
        else if (obj instanceof IoBuffer buffer)
        {
            next.fireWrite(buffer);
        }
        else
        {
            throw new IllegalArgumentException(STR.format("HttpRespPartEncoder不支持入参类型:{}", obj.getClass()));
        }
    }

    private void putBody(HttpRespBody body, IoBuffer buffer)
    {
        if (body.getBodyBuffer() != null && body.getBodyBuffer().remainRead() > 0)
        {
            buffer.put(body.getBodyBuffer());
            body.getBodyBuffer().free();
        }
        else if (body.getBodyBytes() != null)
        {
            buffer.put(body.getBodyBytes());
        }
        else if (body.getBodyText() != null)
        {
            buffer.put(body.getBodyText().getBytes(StandardCharsets.UTF_8));
        }
        else
        {
            throw new IllegalArgumentException("body参数内容是空的");
        }
    }

    private static void putHead(HttpRespHead head, IoBuffer buffer)
    {
        buffer.put(("HTTP/1.1 " + head.getResponseCode() + " OK\n").getBytes(StandardCharsets.US_ASCII));
        head.getHeaders().forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
        buffer.put(NEWLINE);
    }
}
