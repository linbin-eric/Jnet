package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.nio.charset.StandardCharsets;

public class HttpResponseEncoder implements WriteProcessor<HttpResponse>
{
    private final BufferAllocator allocator;

    public HttpResponseEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public void write(HttpResponse data, WriteProcessorNode next)
    {
        IoBuffer buffer = allocator.ioBuffer(1024);
        try
        {
            buffer.put(("HTTP/1.1 " + data.getResponseCode() + " OK\n").getBytes(StandardCharsets.US_ASCII));
            data.getHeaders().forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
            if (data.getBody() == null && data.getBodyBuffer() == null && data.getBytes_body() == null)
            {
                buffer.put(("Content-Length: 0\r\n").getBytes(StandardCharsets.US_ASCII));
                buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
            }
            else if (data.getBodyBuffer() != null && data.getBodyBuffer().remainRead() == 0)
            {
                buffer.put(("Content-Length: 0\r\n").getBytes(StandardCharsets.US_ASCII));
                buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
            }
            else if (data.getBytes_body() != null && data.getBytes_body().length == 0)
            {
                buffer.put(("Content-Length: 0\r\n").getBytes(StandardCharsets.US_ASCII));
                buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
            }
            else
            {
                if (data.isAutoSetContentType())
                {
                    if (data.getContentType() != null && !"".equals(data.getContentType()))
                    {
                        buffer.put(("content-type: " + data.getContentType() + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    }
                    else
                    {
                        buffer.put("content-type: application/json;charset=utf8\r\n".getBytes(StandardCharsets.US_ASCII));
                    }
                }
                if (data.getBodyBuffer() != null)
                {
                    if (data.isAutoSetContentLength())
                    {
                        buffer.put(("Content-Length: " + data.getBodyBuffer().remainRead() + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    }
                    buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
                    buffer.put(data.getBodyBuffer());
                    data.getBodyBuffer().free();
                }
                else
                {
                    byte[] array = data.getBytes_body() != null ? data.getBytes_body() : data.getBody().getBytes(StandardCharsets.UTF_8);
                    if (data.isAutoSetContentLength())
                    {
                        buffer.put(("Content-Length: " + array.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    }
                    buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
                    buffer.put(array);
                }
            }
            next.fireWrite(buffer);
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }
}
