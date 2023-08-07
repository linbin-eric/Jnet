package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.decoder.AbstractDecoder;
import com.jfirer.jnet.common.util.HttpDecodeUtil;

import java.nio.charset.StandardCharsets;

public class HttpReceiveResponseDecoder extends AbstractDecoder
{
    private              HttpReceiveResponse receiveResponse;
    private              ParseState          state     = ParseState.RESPONSE_LINE;
    private              int                 lastCheck = -1;
    private final        byte[]              httpCode  = new byte[3];
    private              int                 bodyRead  = 0;
    private              int                 chunkSize = -1;
    private              int                 chunkHeaderLength;
    private final        HttpConnection      httpConnection;
    private static final byte                re        = "\r".getBytes(StandardCharsets.US_ASCII)[0];
    private static final byte                nl        = "\n".getBytes(StandardCharsets.US_ASCII)[0];

    public HttpReceiveResponseDecoder(HttpConnection httpConnection)
    {
        super(HttpClient.ALLOCATOR);
        this.httpConnection = httpConnection;
    }

    enum ParseState
    {
        RESPONSE_LINE, HEADER, BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (receiveResponse == null)
        {
            receiveResponse = new HttpReceiveResponse(httpConnection);
        }
        boolean goToNextState = false;
        do
        {
            switch (state)
            {
                case RESPONSE_LINE -> goToNextState = decodeResponseLine();
                case HEADER -> goToNextState = decodeHeader(next);
                case BODY_FIX_LENGTH -> goToNextState = decodeBodyWithFixLength();
                case BODY_CHUNKED -> goToNextState = decodeBodyWithChunked();
            }
        } while (goToNextState);
    }

    private boolean decodeBodyWithChunked()
    {
        do
        {
            if (chunkSize == -1)
            {
                for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++)
                {
                    if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n')
                    {
                        chunkSize         = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString(), 16);
                        chunkHeaderLength = i + 2 - accumulation.getReadPosi();
                        break;
                    }
                }
            }
            if (chunkSize == -1 || (chunkSize > 0 && accumulation.remainRead() < chunkHeaderLength + chunkSize + 2))
            {
                return false;
            }
            else if (chunkSize > 0)
            {
                receiveResponse.addPartOfBody(new PartOfBody(2, accumulation.slice(chunkHeaderLength + chunkSize + 2), chunkHeaderLength, chunkSize));
                chunkSize = -1;
                return true;
            }
            else if (chunkSize == 0)
            {
                receiveResponse.addPartOfBody(new PartOfBody(2, accumulation.slice(chunkHeaderLength + 2), chunkHeaderLength, 0));
                receiveResponse.endOfBody();
                receiveResponse = null;
                chunkSize       = -1;
                state           = ParseState.RESPONSE_LINE;
                if (accumulation.remainRead() != 0)
                {
                    throw new IllegalStateException();
                }
                else
                {
                    accumulation.free();
                    accumulation = null;
                }
                return true;
            }
            else
            {
                throw new IllegalArgumentException();
            }
        } while (true);
    }

    private boolean decodeBodyWithFixLength()
    {
        int left = receiveResponse.getContentLength() - bodyRead;
        if (left > accumulation.remainRead())
        {
            bodyRead += accumulation.remainRead();
            receiveResponse.addPartOfBody(new PartOfBody(1, accumulation, 0, 0));
            accumulation = null;
            return false;
        }
        else
        {
            receiveResponse.addPartOfBody(new PartOfBody(1, accumulation.slice(left), 0, 0));
            bodyRead = 0;
            //应用程序已经提前关闭了流，则此时流里可能存在Buffer，需要清空
            receiveResponse.endOfBody();
            receiveResponse = null;
            state           = ParseState.RESPONSE_LINE;
            if (accumulation.remainRead() != 0)
            {
                throw new IllegalStateException();
            }
            accumulation.free();
            accumulation = null;
            return false;
        }
    }

    private boolean decodeHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck = -1;
                state     = ParseState.BODY;
                break;
            }
        }
        if (state == ParseState.BODY)
        {
            HttpDecodeUtil.findAllHeaders(accumulation, receiveResponse::putHeader);
            parseBodyType();
            next.fireRead(receiveResponse);
            return true;
        }
        else
        {
            return false;
        }
    }

    private void parseBodyType()
    {
        HttpDecodeUtil.findContentType(receiveResponse.getHeaders(), receiveResponse::setContentType);
        HttpDecodeUtil.findContentLength(receiveResponse.getHeaders(), receiveResponse::setContentLength);
        if (receiveResponse.getContentLength() == 0)
        {
            if (receiveResponse.getHeaders().entrySet().stream().noneMatch(entry -> entry.getKey().equalsIgnoreCase("Transfer-Encoding")))
            {
                throw new IllegalStateException("无法读取到响应体长度也不是分块传输");
            }
            receiveResponse.setContentLength(-1);
            state = ParseState.BODY_CHUNKED;
        }
        else
        {
            state = ParseState.BODY_FIX_LENGTH;
        }
    }

    private boolean decodeResponseLine()
    {
        lastCheck = lastCheck == -1 ? accumulation.getReadPosi() : lastCheck;
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
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
    public void channelClose(ReadProcessorNode next, Throwable e)
    {
        if (receiveResponse != null)
        {
            receiveResponse.terminate();
        }
        super.channelClose(next, e);
    }
}
