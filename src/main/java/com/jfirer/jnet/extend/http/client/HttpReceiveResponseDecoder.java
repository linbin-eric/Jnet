package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.coder.AbstractDecoder;
import com.jfirer.jnet.common.util.HttpDecodeUtil;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

@Data
public class HttpReceiveResponseDecoder extends AbstractDecoder
{
    private static final byte                                                      CR                         = (byte) '\r';
    private static final byte                                                      LF                         = (byte) '\n';
    private static final int                                                       HTTP_VERSION_PREFIX_LENGTH = 9; // "HTTP/1.1 "
    private static final int                                                       HTTP_CODE_LENGTH           = 3;
    private final        byte[]                                                    httpCode                   = new byte[HTTP_CODE_LENGTH];
    private              HttpReceiveResponse                                       receiveResponse;
    private              ParseState                                                state                      = ParseState.RESPONSE_LINE;
    private              int                                                       lastCheck                  = -1;
    private              int                                                       bodyRead                   = 0;
    private              int                                                       chunkSize                  = -1;
    private              int                                                       chunkHeaderLength;
    private final        HttpConnection                                            httpConnection;
    private final        BiFunction<Pipeline, HttpConnection, HttpReceiveResponse> responseCreator;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (receiveResponse == null)
        {
            receiveResponse = responseCreator.apply(next.pipeline(), httpConnection);
        }
        boolean goToNextState = false;
        do
        {
            switch (state)
            {
                case RESPONSE_LINE -> goToNextState = decodeResponseLine();
                case HEADER -> goToNextState = decodeHeader(next);
                case BODY_FIX_LENGTH -> goToNextState = decodeBodyWithFixLength();
                case NO_BODY ->
                {
                    goToNextState = false;
                    endThisRound();
                }
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
                for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi() - 1; i++)
                {
                    if (accumulation.get(i) == CR && accumulation.get(i + 1) == LF)
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
                receiveResponse.addPartOfBody(new ChunkedPart(chunkHeaderLength, chunkSize, accumulation.slice(chunkHeaderLength + chunkSize + 2)));
                chunkSize = -1;
                return true;
            }
            else if (chunkSize == 0)
            {
                receiveResponse.addPartOfBody(new ChunkedPart(chunkHeaderLength, 0, accumulation.slice(chunkHeaderLength + 2)));
                chunkSize = -1;
                endThisRound();
                return false;
            }
            else
            {
                throw new IllegalArgumentException();
            }
        } while (true);
    }

    private boolean decodeBodyWithFixLength()
    {
        int left   = receiveResponse.getContentLength() - bodyRead;
        int remain = accumulation.remainRead();
        bodyRead += remain;
        receiveResponse.addPartOfBody(new FixLengthPart(accumulation));
        accumulation = null;
        if (left > remain)
        {
            return false;
        }
        else if (left == remain)
        {
            endThisRound();
            return false;
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    private void endThisRound()
    {
        bodyRead = 0;
        receiveResponse.endOfBody();
        receiveResponse = null;
        state           = ParseState.RESPONSE_LINE;
        if (accumulation != null)
        {
            if (accumulation.remainRead() != 0)
            {
                throw new IllegalStateException();
            }
            accumulation.free();
            accumulation = null;
        }
    }

    private boolean decodeHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == CR && accumulation.get(lastCheck + 1) == LF && accumulation.get(lastCheck + 2) == CR && accumulation.get(lastCheck + 3) == LF)
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
                state = ParseState.NO_BODY;
            }
            else
            {
                state = ParseState.BODY_CHUNKED;
                receiveResponse.setContentLength(-1);
            }
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
            if (accumulation.get(lastCheck) == CR && accumulation.get(lastCheck + 1) == LF)
            {
                lastCheck += 2;
                state = ParseState.HEADER;
                break;
            }
        }
        if (state == ParseState.HEADER)
        {
            accumulation.get(httpCode, 0, HTTP_CODE_LENGTH, accumulation.getReadPosi() + HTTP_VERSION_PREFIX_LENGTH);
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
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (receiveResponse != null)
        {
            receiveResponse.endOfBody();
        }
        super.readFailed(e, next);
    }

    enum ParseState
    {
        RESPONSE_LINE, HEADER, BODY, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
