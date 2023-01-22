package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.nio.charset.StandardCharsets;

public class HttpResponseEncoder implements WriteProcessor<HttpResponse>
{
    private BufferAllocator allocator;

    public HttpResponseEncoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public void write(HttpResponse data, WriteProcessorNode next)
    {
        IoBuffer buffer = allocator.ioBuffer(1024);
        buffer.put("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.US_ASCII));
        data.getHeaders().forEach((name, value) -> {
            buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII));
        });
        buffer.put("content-type: application/json;charset=utf8\r\n".getBytes(StandardCharsets.US_ASCII));
        byte[] array = data.getBody().getBytes(StandardCharsets.UTF_8);
        buffer.put(("Content-Length: " + array.length + "\r\n").getBytes(StandardCharsets.US_ASCII));
        buffer.put("\r\n".getBytes(StandardCharsets.US_ASCII));
        buffer.put(array);
        next.fireWrite(buffer);
    }
}
