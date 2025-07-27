package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.nio.charset.StandardCharsets;

public class HttpSendRequestEncoder implements WriteProcessor<HttpSendRequest>
{
    private static final byte[] HTTP_VERSION    = "HTTP/1.1".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NEW_LINE        = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HEADER_INTERVAL = ": ".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CONTENT_LENGTH  = "Content-Length".getBytes(StandardCharsets.US_ASCII);

    @Override
    public void write(HttpSendRequest request, WriteProcessorNode next)
    {
        IoBuffer buffer = next.pipeline().allocator().allocate(1024);
        buffer.put((request.getMethod() + " " + request.getPath() + " HTTP/1.1\r\n").getBytes(StandardCharsets.US_ASCII));
        request.putHeader("Connection", "keep-alive");
        request.putHeader("User-Agent", "JnetHttpClient");
        request.putHeader("Accept", "*/*");
        if (request.getContentType() != null)
        {
            request.putHeader("Content-Type", request.getContentType());
        }
        int contentLength = 0;
        // 优先级判断：IoBuffer > String
        if (request.getBodyBuffer() != null)
        {
            contentLength = request.getBodyBuffer().remainRead();
        }
        else if (request.getBodyString() != null)
        {
            byte[] bodyBytes = request.getBodyString().getBytes(StandardCharsets.UTF_8);
            contentLength = bodyBytes.length;
            if (contentLength > 0)
            {
                IoBuffer bodyBuffer = next.pipeline().allocator().allocate(contentLength);
                bodyBuffer.put(bodyBytes);
                request.setBody(bodyBuffer);
            }
        }
        request.putHeader("Content-Length", String.valueOf(contentLength));
        request.getHeaders().forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
        buffer.put(NEW_LINE);
        if (contentLength > 0)
        {
            buffer.put(request.getBodyBuffer());
        }
        request.close();
        next.fireWrite(buffer);
    }
}
