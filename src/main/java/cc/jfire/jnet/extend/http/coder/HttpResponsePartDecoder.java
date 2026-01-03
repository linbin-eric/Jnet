package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpResponseChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpResponseFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartEnd;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;

import java.nio.charset.StandardCharsets;

public class HttpResponsePartDecoder extends AbstractDecoder
{
    private static final int                  MAX_CHUNK_SIZE_LINE_LENGTH = 32;
    private              int                  lastCheck                  = -1;
    private              ParseState           state                      = ParseState.RESPONSE_LINE;
    private              HttpResponsePartHead respHead;
    private              int                  headStartPosi              = -1;
    private              int                  bodyRead                   = 0;
    private              int                  chunkSize                  = -1;
    private              int                  chunkSizeLineLength        = -1;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        boolean goToNextState;
        do
        {
            goToNextState = switch (state)
            {
                case RESPONSE_LINE -> parseResponseLine();
                case RESPONSE_HEADER -> parseResponseHeader(next);
                case BODY_FIX_LENGTH -> parseBodyFixLength(next);
                case BODY_CHUNKED -> parseBodyChunked(next);
                case NO_BODY ->
                {
                    next.fireRead(new HttpResponsePartEnd());
                    resetState();
                    yield accumulation != null && accumulation.remainRead() > 0;
                }
            };
        } while (goToNextState);
    }

    private boolean parseResponseLine()
    {
        if (lastCheck == -1)
        {
            lastCheck = accumulation.getReadPosi();
            headStartPosi = lastCheck;
        }
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
            {
                lastCheck += 2;
                state = ParseState.RESPONSE_HEADER;
                break;
            }
        }
        if (state == ParseState.RESPONSE_HEADER)
        {
            respHead = new HttpResponsePartHead();
            decodeResponseLine();
            return true;
        }
        return false;
    }

    private void decodeResponseLine()
    {
        // 解析 version（如 HTTP/1.1）
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                respHead.setVersion(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        // 解析 statusCode（如 200）
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                respHead.setStatusCode(Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString()));
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        // 解析 reasonPhrase（如 OK）
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == '\r')
            {
                respHead.setReasonPhrase(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                break;
            }
        }
        accumulation.setReadPosi(lastCheck);
    }

    private boolean parseResponseHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck = -1;
                HttpDecodeUtil.findAllHeaders(accumulation, respHead::addHeader);
                HttpDecodeUtil.findContentLength(respHead.getHeaders(), respHead::setContentLength);
                int bodyStartPosi = accumulation.getReadPosi();
                int headLength = bodyStartPosi - headStartPosi;
                if (headLength > 0)
                {
                    accumulation.setReadPosi(headStartPosi);
                    respHead.setPart(accumulation.slice(headLength));
                }
                parseBodyType();
                next.fireRead(respHead);
                return true;
            }
        }
        return false;
    }

    private void parseBodyType()
    {
        if (respHead.getContentLength() > 0)
        {
            state = ParseState.BODY_FIX_LENGTH;
        }
        else
        {
            boolean hasTransferEncoding = respHead.getHeaders().entrySet().stream().anyMatch(e -> e.getKey().equalsIgnoreCase("Transfer-Encoding") && e.getValue().equalsIgnoreCase("chunked"));
            if (hasTransferEncoding)
            {
                state = ParseState.BODY_CHUNKED;
                respHead.setChunked(true);
            }
            else
            {
                state = ParseState.NO_BODY;
            }
        }
    }

    private boolean parseBodyFixLength(ReadProcessorNode next)
    {
        if (accumulation == null || accumulation.remainRead() == 0)
        {
            return false;
        }
        int left   = respHead.getContentLength() - bodyRead;
        int remain = accumulation.remainRead();
        if (remain > left)
        {
            HttpResponseFixLengthBodyPart part = new HttpResponseFixLengthBodyPart();
            part.setPart(accumulation.slice(left));
            next.fireRead(part);
            next.fireRead(new HttpResponsePartEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        else if (remain == left)
        {
            HttpResponseFixLengthBodyPart part = new HttpResponseFixLengthBodyPart();
            part.setPart(accumulation);
            accumulation = null;
            next.fireRead(part);
            next.fireRead(new HttpResponsePartEnd());
            resetState();
            return false;
        }
        else
        {
            bodyRead += remain;
            HttpResponseFixLengthBodyPart part = new HttpResponseFixLengthBodyPart();
            part.setPart(accumulation);
            accumulation = null;
            next.fireRead(part);
            return false;
        }
    }

    private boolean parseBodyChunked(ReadProcessorNode next)
    {
        if (chunkSize == -1)
        {
            int startPosi = accumulation.getReadPosi();
            for (int i = startPosi; i < accumulation.getWritePosi() - 1; i++)
            {
                if (i - startPosi > MAX_CHUNK_SIZE_LINE_LENGTH)
                {
                    throw new IllegalStateException("Chunk size line exceeds " + MAX_CHUNK_SIZE_LINE_LENGTH + " bytes");
                }
                if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n')
                {
                    chunkSize = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString().trim(), 16);
                    chunkSizeLineLength = i + 2 - startPosi;
                    break;
                }
            }
        }
        if (chunkSize == -1)
        {
            return false;
        }
        if (chunkSize == 0)
        {
            int totalLength = chunkSizeLineLength + 2;
            if (accumulation.remainRead() < totalLength)
            {
                return false;
            }
            accumulation.addReadPosi(totalLength);
            next.fireRead(new HttpResponsePartEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }

        int totalChunkLength = chunkSizeLineLength + chunkSize + 2;
        if (accumulation.remainRead() < totalChunkLength)
        {
            return false;
        }

        HttpResponseChunkedBodyPart part = new HttpResponseChunkedBodyPart();
        part.setHeadLength(chunkSizeLineLength);
        part.setChunkLength(totalChunkLength);
        part.setPart(accumulation.slice(totalChunkLength));

        next.fireRead(part);
        chunkSize = -1;
        chunkSizeLineLength = -1;
        return true;
    }

    private void resetState()
    {
        respHead            = null;
        headStartPosi       = -1;
        bodyRead            = 0;
        chunkSize           = -1;
        chunkSizeLineLength = -1;
        state               = ParseState.RESPONSE_LINE;
        if (accumulation != null && accumulation.remainRead() == 0)
        {
            accumulation.free();
            accumulation = null;
        }
    }

    enum ParseState
    {
        RESPONSE_LINE, RESPONSE_HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
