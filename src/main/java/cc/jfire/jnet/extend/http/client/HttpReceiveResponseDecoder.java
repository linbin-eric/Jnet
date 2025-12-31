package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
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
    private static final int                                                       MAX_CHUNK_SIZE_LINE_LENGTH = 32;
    private static final int                                                       CHUNK_TRAILING_CRLF_LENGTH = 2;
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
                    if (i - accumulation.getReadPosi() > MAX_CHUNK_SIZE_LINE_LENGTH)
                    {
                        throw new IllegalStateException("Chunk size line exceeds " + MAX_CHUNK_SIZE_LINE_LENGTH + " bytes");
                    }
                    if (accumulation.get(i) == CR && accumulation.get(i + 1) == LF)
                    {
                        try
                        {
                            chunkSize = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString().trim(), 16);
                        }
                        catch (NumberFormatException e)
                        {
                            throw new IllegalStateException("Invalid chunk size format", e);
                        }
                        chunkHeaderLength = i + CHUNK_TRAILING_CRLF_LENGTH - accumulation.getReadPosi();
                        break;
                    }
                }
            }
            if (chunkSize == -1 || (chunkSize > 0 && accumulation.remainRead() < chunkHeaderLength + chunkSize + CHUNK_TRAILING_CRLF_LENGTH))
            {
                return false;
            }
            else if (chunkSize > 0)
            {
                receiveResponse.addPartOfBody(new ChunkedPart(chunkHeaderLength, chunkSize, accumulation.slice(chunkHeaderLength + chunkSize + CHUNK_TRAILING_CRLF_LENGTH)));
                chunkSize = -1;
                return true;
            }
            else if (chunkSize == 0)
            {
                receiveResponse.addPartOfBody(new ChunkedPart(chunkHeaderLength, 0, accumulation.slice(chunkHeaderLength + CHUNK_TRAILING_CRLF_LENGTH)));
                chunkSize = -1;
                endThisRound();
                return false;
            }
            else
            {
                throw new IllegalArgumentException("Chunk size cannot be negative: " + chunkSize);
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
            throw new IllegalStateException("Body content length mismatch: expected " + receiveResponse.getContentLength() + " bytes, but got more data");
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
                throw new IllegalStateException("Accumulation buffer should be fully consumed at the end of response");
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
                HttpDecodeUtil.findAllHeaders(accumulation, receiveResponse::putHeader);
                parseBodyType();
                next.fireRead(receiveResponse);
                return true;
            }
        }
        return false;
    }

    private void parseBodyType()
    {
        HttpDecodeUtil.findContentType(receiveResponse.getHeaders(), receiveResponse::setContentType);
        HttpDecodeUtil.findContentLength(receiveResponse.getHeaders(), receiveResponse::setContentLength);
        if (receiveResponse.getContentLength() == 0)
        {
            boolean hasTransferEncoding = false;
            for (java.util.Map.Entry<String, String> entry : receiveResponse.getHeaders().entrySet())
            {
                if (entry.getKey().equalsIgnoreCase("Transfer-Encoding"))
                {
                    hasTransferEncoding = true;
                    break;
                }
            }
            if (!hasTransferEncoding)
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
        RESPONSE_LINE, HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
