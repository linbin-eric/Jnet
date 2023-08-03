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
        IoBuffer buffer = HttpClient.ALLOCATOR.ioBuffer(1024);
        buffer.put((request.getMethod() + " " + request.getPath() + " HTTP/1.1\r\n").getBytes(StandardCharsets.US_ASCII));
        request.putHeader("Connection", "keep-alive");
        request.putHeader("User-Agent", "JnetHttpClient");
        request.putHeader("Accept", "*/*");
        if (request.getContentType() != null)
        {
            request.putHeader("Content-Type", request.getContentType());
        }
        IoBuffer body = request.getBody();
        if (body != null)
        {
            request.putHeader("Content-Length", String.valueOf(body.remainRead()));
        }
        else
        {
            request.putHeader("Content-Length", "0");
        }
        request.getHeaders().forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
        buffer.put(NEW_LINE);
        if (body != null)
        {
            buffer.put(body);
            body.free();
        }
        next.fireWrite(buffer);
    }
}
