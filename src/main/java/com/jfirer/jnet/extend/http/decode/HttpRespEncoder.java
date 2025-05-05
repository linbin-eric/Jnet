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

public class HttpRespEncoder implements WriteProcessor<Object>
{
    private final       BufferAllocator allocator;
    public static final byte[]          NEWLINE          = "\r\n".getBytes(StandardCharsets.US_ASCII);
    public static final byte[]          APPLICATION_JSON = "content-type: application/json;charset=utf8\r\n".getBytes(StandardCharsets.US_ASCII);

    public HttpRespEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public void write(Object obj, WriteProcessorNode next)
    {
        if (obj instanceof HttpRespHead head)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            head.write(buffer);
            buffer.put(NEWLINE);
            next.fireWrite(buffer);
        }
        else if (obj instanceof HttpRespBody body)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            putBody(body, buffer, false);
            next.fireWrite(buffer);
        }
        else if (obj instanceof FullHttpResp fullHttpResp)
        {
            IoBuffer buffer = allocator.ioBuffer(1024);
            try
            {
                fullHttpResp.getHead().write(buffer);
                if (fullHttpResp.getHead().hasContentType() == false)
                {
                    buffer.put(APPLICATION_JSON);
                }
                putBody(fullHttpResp.getBody(), buffer, fullHttpResp.getHead().hasContentLength() == false);
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

    private void putBody(HttpRespBody body, IoBuffer buffer, boolean setLength)
    {
        if (body.getBodyBuffer() != null && body.getBodyBuffer().remainRead() > 0)
        {
            if (setLength)
            {
                buffer.put(STR.format("Content-Length: {}\r\n", body.getBodyBuffer().remainRead()).getBytes(StandardCharsets.US_ASCII));
                buffer.put(NEWLINE);
            }
            buffer.put(body.getBodyBuffer());
            body.getBodyBuffer().free();
        }
        else if (body.getBodyBytes() != null)
        {
            if (setLength)
            {
                buffer.put(STR.format("Content-Length: {}\r\n", body.getBodyBytes().length).getBytes(StandardCharsets.US_ASCII));
                buffer.put(NEWLINE);
            }
            buffer.put(body.getBodyBytes());
        }
        else if (body.getBodyText() != null)
        {
            byte[] array = body.getBodyText().getBytes(StandardCharsets.UTF_8);
            if (setLength)
            {
                buffer.put(STR.format("Content-Length: {}\r\n", array.length).getBytes(StandardCharsets.US_ASCII));
                buffer.put(NEWLINE);
            }
            buffer.put(array);
        }
        else
        {
            throw new IllegalArgumentException("body参数内容是空的");
        }
    }
}
