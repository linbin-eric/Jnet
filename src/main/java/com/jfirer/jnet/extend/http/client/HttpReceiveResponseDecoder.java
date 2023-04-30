package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.decoder.AbstractDecoder;
import com.jfirer.jnet.extend.http.decode.ContentType;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class HttpReceiveResponseDecoder extends AbstractDecoder
{
    private              HttpReceiveResponse receiveResponse;
    private              ParseState          state      = ParseState.RESPONSE_LINE;
    private              int                 lastCheck  = -1;
    private final        byte[]              httpCode   = new byte[3];
    private              int                 bodyRead   = 0;
    private              int                 chunkSize  = -1;
    /**
     * 响应体类型是二进制数据
     */
    private              boolean             streamBody = false;
    private static final byte                re         = "\r".getBytes(StandardCharsets.US_ASCII)[0];
    private static final byte                nl         = "\n".getBytes(StandardCharsets.US_ASCII)[0];

    public HttpReceiveResponseDecoder()
    {
        super(HttpClient.ALLOCATOR);
    }

    enum ParseState
    {
        RESPONSE_LINE,
        HEADER,
        BODY,
        BODY_FIX_LENGTH,
        BODY_CHUNKED
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (receiveResponse == null)
        {
            receiveResponse = new HttpReceiveResponse();
        }
        boolean nextRound = false;
        do
        {
            switch (state)
            {
                case RESPONSE_LINE -> nextRound = decodeResponseLine();
                case HEADER -> nextRound = decodeHeader(next);
                case BODY_FIX_LENGTH ->
                        nextRound = decodeBodyWithFixLength(next);
                case BODY_CHUNKED -> nextRound = decodeBodyWithChunked(next);
            }
        }
        while (nextRound);
    }

    private boolean decodeBodyWithChunked(ReadProcessorNode next)
    {
        if (chunkSize == -1)
        {
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
            {
                if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n')
                {
                    int mark = accumulation.getWritePosi();
                    accumulation.setWritePosi(i);
                    chunkSize = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer()).toString(), 16);
                    accumulation.setWritePosi(mark);
                    accumulation.setReadPosi(i + 2);
                    break;
                }
            }
        }
        if (chunkSize == -1 || (chunkSize > 0 && accumulation.remainRead() < chunkSize + 2))
        {
            return false;
        }
        else if (chunkSize > 0)
        {
            receiveResponse.getBody().put(accumulation, chunkSize);
            accumulation.addReadPosi(chunkSize + 2);
            chunkSize = -1;
            return false;
        }
        else if (chunkSize == 0)
        {
            next.fireRead(receiveResponse);
            receiveResponse = null;
            lastCheck = -1;
            chunkSize = -1;
            state = ParseState.RESPONSE_LINE;
            accumulation.addReadPosi(2);
            compactIfNeed();
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean decodeBodyWithFixLength(ReadProcessorNode next)
    {
        if (streamBody)
        {
            IoBuffer fragment = allocator.ioBuffer(accumulation.remainRead());
            accumulation.get(fragment, accumulation.remainRead());
            accumulation.compact();
            receiveResponse.getStream().offer(fragment);
            bodyRead += fragment.getWritePosi();
            if (bodyRead >= receiveResponse.getContentLength())
            {
                bodyRead = 0;
                //应用程序已经提前关闭了流，则此时流里可能存在Buffer，需要清空
                if (receiveResponse.isClosed())
                {
                    receiveResponse.clearStream();
                }
                receiveResponse.getStream().offer(HttpReceiveResponse.END_OF_STREAM);
                receiveResponse = null;
                streamBody = false;
                lastCheck = -1;
                state = ParseState.RESPONSE_LINE;
                compactIfNeed();
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            if (accumulation.remainRead() >= receiveResponse.getContentLength())
            {
                receiveResponse.setBody(accumulation.slice(receiveResponse.getContentLength()));
                next.fireRead(receiveResponse);
                receiveResponse = null;
                lastCheck = -1;
                state = ParseState.RESPONSE_LINE;
                compactIfNeed();
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private boolean decodeHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck += 4;
                state = ParseState.BODY;
                break;
            }
        }
        if (state == ParseState.BODY)
        {
            findAllHeaders();
            parseBodyType();
            if (receiveResponse.getContentType().toLowerCase().startsWith(ContentType.STREAM))
            {
                streamBody = true;
                accumulation.capacityReadyFor(1024 * 1024 * 2);
                receiveResponse.setStream(new LinkedBlockingDeque<>());
                next.fireRead(receiveResponse);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    private void parseBodyType()
    {
        receiveResponse.getHeaders().entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("content-length")).findFirst().ifPresent(v -> receiveResponse.setContentLength(Integer.parseInt(v.getValue())));
        receiveResponse.getHeaders().entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type")).findFirst().ifPresent(v -> receiveResponse.setContentType(v.getValue()));
        if (receiveResponse.getContentLength() == 0)
        {
            if (!receiveResponse.getHeaders().entrySet().stream().anyMatch(entry -> entry.getKey().equalsIgnoreCase("Transfer-Encoding")))
            {
                throw new IllegalStateException("无法读取到响应体长度也不是分块传输");
            }
            state = ParseState.BODY_CHUNKED;
            receiveResponse.setBody(allocator.ioBuffer(1024));
        }
        else
        {
            state = ParseState.BODY_FIX_LENGTH;
        }
    }

    private void findAllHeaders()
    {
        String headerName = null, headerValue = null;
        while (accumulation.get(accumulation.getReadPosi()) != '\r' || accumulation.get(accumulation.getReadPosi() + 1) != '\n')
        {
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
            {
                if (accumulation.get(i) == ':')
                {
                    int mark = accumulation.getWritePosi();
                    accumulation.setWritePosi(i);
                    headerName = StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer()).toString();
                    accumulation.setWritePosi(mark);
                    accumulation.setReadPosi(i + 2);
                    break;
                }
            }
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
            {
                if (accumulation.get(i) == '\r')
                {
                    int mark = accumulation.getWritePosi();
                    accumulation.setWritePosi(i);
                    headerValue = StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer()).toString();
                    accumulation.setWritePosi(mark);
                    accumulation.setReadPosi(i + 2);
                    break;
                }
            }
            receiveResponse.putHeader(headerName, headerValue);
        }
        accumulation.addReadPosi(2);
    }

    private boolean decodeResponseLine()
    {
        lastCheck = lastCheck == -1 ? accumulation.getReadPosi() : lastCheck;
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == re && accumulation.get(lastCheck + 1) == nl)
            {
                lastCheck += 2;
                state = ParseState.HEADER;
                break;
            }
        }
        if (state == ParseState.HEADER)
        {
            accumulation.get(httpCode, 0, 3, accumulation.getReadPosi() + 9);
            receiveResponse.setHttpCode(Integer.parseInt(new String(httpCode, StandardCharsets.US_ASCII)));
            accumulation.setReadPosi(lastCheck);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void channelClose(ReadProcessorNode next)
    {
        try
        {
            if (receiveResponse != null)
            {
                receiveResponse.close();
                BlockingQueue<IoBuffer> stream = receiveResponse.getStream();
                if (stream != null)
                {
                    stream.offer(HttpReceiveResponse.CLOSE_OF_CHANNEL);
                }
            }
        }
        catch (Exception e)
        {
            ;
        }
        super.channelClose(next);
    }
}
