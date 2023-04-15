package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.decoder.AbstractDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRequestDecoder extends AbstractDecoder
{
    static final int         REQUEST_LINE   = 1;
    static final int         REQUEST_HEADER = 2;
    static final int         REQUEST_BODY   = 3;
    private      int         lastCheck      = -1;
    private      int         state          = REQUEST_LINE;
    private      HttpRequest decodeObject;

    public HttpRequestDecoder(BufferAllocator allocator)
    {
        super(allocator);
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (decodeObject == null)
        {
            decodeObject = new HttpRequest();
        }
        switch (state)
        {
            case REQUEST_LINE ->
            {
                lastCheck = lastCheck == -1 ? accumulation.getReadPosi() : lastCheck;
                for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
                {
                    if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
                    {
                        lastCheck += 2;
                        state = REQUEST_HEADER;
                        break;
                    }
                }
                if (state == REQUEST_HEADER)
                {
                    decodeRequestLine();
                    process0(next);
                }
                else
                {
                    return;
                }
            }
            case REQUEST_HEADER ->
            {
                for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
                {
                    if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
                    {
                        lastCheck += 4;
                        state = REQUEST_BODY;
                        break;
                    }
                }
                if (state == REQUEST_BODY)
                {
                    decodeHeaders();
                    process0(next);
                }
                else
                {
                    ;
                }
            }
            case REQUEST_BODY ->
            {
                if (decodeObject.getContentLength() != 0)
                {
                    if (accumulation.remainRead() < decodeObject.getContentLength())
                    {
                        return;
                    }
                    else
                    {
                        IoBuffer requestBody = accumulation.slice(decodeObject.getContentLength());
                        decodeObject.setBody(requestBody);
                    }
                }
                next.fireRead(decodeObject);
                decodeObject = null;
                state = REQUEST_LINE;
                lastCheck = -1;
                compactIfNeed();
                process0(next);
            }
        }
    }

    private void decodeRequestLine()
    {
        IoBuffer requestLine = accumulation.slice(lastCheck - accumulation.getReadPosi());
        decodeHttpMethod(requestLine);
        decodeHttpUrl(requestLine);
        decodeHttpVersion(requestLine);
        requestLine.free();
    }

    private void decodeHeaders()
    {
        IoBuffer requestHeaders = accumulation.slice(lastCheck - accumulation.getReadPosi());
        for (int i = 0; i < requestHeaders.getWritePosi() && requestHeaders.remainRead() > 2; i++)
        {
            if (requestHeaders.get(i) == '\r' && requestHeaders.get(i + 1) == '\n')
            {
                String headerName  = null;
                String headerValue = null;
                for (int j = requestHeaders.getReadPosi(); j < i; j++)
                {
                    if (requestHeaders.get(j) == ':' && requestHeaders.get(j + 1) == ' ')
                    {
                        byte[] headerNameContent = new byte[j - requestHeaders.getReadPosi()];
                        requestHeaders.get(headerNameContent);
                        headerName = new String(headerNameContent, StandardCharsets.US_ASCII);
                        requestHeaders.setReadPosi(j + 2);
                        byte[] headerValueContent = new byte[i - requestHeaders.getReadPosi()];
                        requestHeaders.get(headerValueContent);
                        headerValue = new String(headerValueContent, StandardCharsets.US_ASCII);
                        requestHeaders.setReadPosi(i + 2);
                        break;
                    }
                }
                i = requestHeaders.getReadPosi();
                decodeObject.addHeader(headerName, headerValue);
                if (headerName.equalsIgnoreCase("content-length"))
                {
                    decodeObject.setContentLength(Integer.valueOf(headerValue));
                }
                if (headerName.equalsIgnoreCase("content-type"))
                {
                    decodeObject.setContentType(headerValue);
                }
            }
        }
        requestHeaders.free();
    }

    private void decodeHttpVersion(IoBuffer requestLine)
    {
        int readPosi = requestLine.getReadPosi();
        for (int i = requestLine.getReadPosi(); i < requestLine.getWritePosi(); i++)
        {
            if (requestLine.get(i) == '\r')
            {
                byte[] version = new byte[i - readPosi];
                requestLine.get(version);
                decodeObject.setVersion(new String(version, StandardCharsets.US_ASCII));
                return;
            }
        }
        throw new IllegalStateException();
    }

    private void decodeHttpUrl(IoBuffer requestLine)
    {
        int readPosi = requestLine.getReadPosi();
        for (int i = readPosi; i < requestLine.getWritePosi(); i++)
        {
            if (requestLine.get(i) == ' ')
            {
                byte[] url = new byte[i - readPosi];
                requestLine.get(url);
                decodeObject.setUrl(new String(url, StandardCharsets.US_ASCII));
                requestLine.setReadPosi(i + 1);
                return;
            }
        }
        throw new IllegalStateException();
    }

    private void decodeHttpMethod(IoBuffer requestLine)
    {
        for (int i = 0; i < requestLine.getWritePosi(); i++)
        {
            if (requestLine.get(i) == ' ')
            {
                if (requestLine.get(0) == 'G' && (i) == 3)
                {
                    decodeObject.setMethod("GET");
                }
                else if (requestLine.get(0) == 'P' && (i) == 3)
                {
                    decodeObject.setMethod("PUT");
                }
                else if (requestLine.get(0) == 'P' && (i) == 4)
                {
                    decodeObject.setMethod("POST");
                }
                else if (requestLine.get(0) == 'H' && (i) == 4)
                {
                    decodeObject.setMethod("HEAD");
                }
                else if (requestLine.get(0) == 'D' && (i) == 6)
                {
                    decodeObject.setMethod("DELETE");
                }
                else if (requestLine.get(0) == 'O' && (i) == 7)
                {
                    decodeObject.setMethod("OPTIONS");
                }
                else if (requestLine.get(0) == 'T' && (i) == 5)
                {
                    decodeObject.setMethod("TRACE");
                }
                else if (requestLine.get(0) == 'C' && (i) == 7)
                {
                    decodeObject.setMethod("CONNECT");
                }
                else if (requestLine.get(0) == 'L' && (i) == 4)
                {
                    decodeObject.setMethod("LINK");
                }
                else if (requestLine.get(0) == 'U' && (i) == 6)
                {
                    decodeObject.setMethod("UNLINK");
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
                requestLine.setReadPosi(i + 1);
                return;
            }
        }
        throw new IllegalStateException();
    }
}
