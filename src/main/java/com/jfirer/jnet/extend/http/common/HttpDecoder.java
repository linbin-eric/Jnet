package com.jfirer.jnet.extend.http.common;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.decoder.AbstractDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class HttpDecoder extends AbstractDecoder
{
    public HttpDecoder(BufferAllocator allocator)
    {
        super(allocator);
    }

    protected void findAllHeaders(BiConsumer<String, String> consumer)
    {
        String headerName = null, headerValue = null;
        while (accumulation.get(accumulation.getReadPosi()) != '\r' || accumulation.get(accumulation.getReadPosi() + 1) != '\n')
        {
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
            {
                if (accumulation.get(i) == ':')
                {
                    headerName = StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString();
                    accumulation.setReadPosi(i + 2);
                    break;
                }
            }
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
            {
                if (accumulation.get(i) == '\r')
                {
                    headerValue = StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString();
                    accumulation.setReadPosi(i + 2);
                    break;
                }
            }
            consumer.accept(headerName, headerValue);
        }
        accumulation.addReadPosi(2);
    }

    protected void findContentType(Map<String, String> headers, Consumer<String> contentTypeConsumer)
    {
        headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type")).map(Map.Entry::getValue).findFirst().ifPresent(contentTypeConsumer);
    }

    protected void findContentLength(Map<String, String> headers, Consumer<Integer> contentLengthConsumer)
    {
        headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Content-Length")).map(entry -> Integer.valueOf(entry.getValue())).findFirst().ifPresent(contentLengthConsumer);
    }
}
