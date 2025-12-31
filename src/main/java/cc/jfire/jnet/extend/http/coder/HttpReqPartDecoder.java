package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpReqBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpReqEnd;
import cc.jfire.jnet.extend.http.dto.HttpReqHead;

import java.nio.charset.StandardCharsets;

public class HttpReqPartDecoder extends AbstractDecoder
{
    private static final int MAX_CHUNK_SIZE_LINE_LENGTH = 32;
    private int         lastCheck = -1;
    private ParseState  state     = ParseState.REQUEST_LINE;
    private HttpReqHead reqHead;
    private int         bodyRead  = 0;
    private int         chunkSize = -1;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        boolean goToNextState;
        do
        {
            goToNextState = switch (state)
            {
                case REQUEST_LINE -> parseRequestLine();
                case REQUEST_HEADER -> parseRequestHeader(next);
                case BODY_FIX_LENGTH -> parseBodyFixLength(next);
                case BODY_CHUNKED -> parseBodyChunked(next);
                case NO_BODY ->
                {
                    next.fireRead(new HttpReqEnd());
                    resetState();
                    yield accumulation != null && accumulation.remainRead() > 0;
                }
            };
        } while (goToNextState);
    }

    private boolean parseRequestLine()
    {
        if (lastCheck == -1)
        {
            lastCheck = accumulation.getReadPosi();
        }
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
            {
                lastCheck += 2;
                state = ParseState.REQUEST_HEADER;
                break;
            }
        }
        if (state == ParseState.REQUEST_HEADER)
        {
            reqHead = new HttpReqHead();
            decodeRequestLine();
            return true;
        }
        return false;
    }

    private void decodeRequestLine()
    {
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                reqHead.setMethod(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                reqHead.setUrl(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == '\r')
            {
                reqHead.setVersion(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                break;
            }
        }
        accumulation.setReadPosi(lastCheck);
    }

    private boolean parseRequestHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n'
                && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck = -1;
                HttpDecodeUtil.findAllHeaders(accumulation, reqHead::addHeader);
                HttpDecodeUtil.findContentLength(reqHead.getHeaders(), reqHead::setContentLength);
                parseBodyType();
                next.fireRead(reqHead);
                return true;
            }
        }
        return false;
    }

    private void parseBodyType()
    {
        if (reqHead.getContentLength() > 0)
        {
            state = ParseState.BODY_FIX_LENGTH;
        }
        else
        {
            boolean hasTransferEncoding = reqHead.getHeaders().entrySet().stream()
                .anyMatch(e -> e.getKey().equalsIgnoreCase("Transfer-Encoding") && e.getValue().equalsIgnoreCase("chunked"));
            if (hasTransferEncoding)
            {
                state = ParseState.BODY_CHUNKED;
                reqHead.setChunked(true);
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
        int left = reqHead.getContentLength() - bodyRead;
        int remain = accumulation.remainRead();
        if (remain > left)
        {
            HttpReqBodyPart part = new HttpReqBodyPart();
            part.setPart(accumulation.slice(left));
            next.fireRead(part);
            next.fireRead(new HttpReqEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        else if (remain == left)
        {
            HttpReqBodyPart part = new HttpReqBodyPart();
            part.setPart(accumulation);
            accumulation = null;
            next.fireRead(part);
            next.fireRead(new HttpReqEnd());
            resetState();
            return false;
        }
        else
        {
            bodyRead += remain;
            HttpReqBodyPart part = new HttpReqBodyPart();
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
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi() - 1; i++)
            {
                if (i - accumulation.getReadPosi() > MAX_CHUNK_SIZE_LINE_LENGTH)
                {
                    throw new IllegalStateException("Chunk size line exceeds " + MAX_CHUNK_SIZE_LINE_LENGTH + " bytes");
                }
                if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n')
                {
                    chunkSize = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString().trim(), 16);
                    accumulation.setReadPosi(i + 2);
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
            if (accumulation.remainRead() < 2)
            {
                return false;
            }
            accumulation.addReadPosi(2);
            next.fireRead(new HttpReqEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        if (accumulation.remainRead() < chunkSize + 2)
        {
            return false;
        }
        HttpReqBodyPart part = new HttpReqBodyPart();
        part.setPart(accumulation.slice(chunkSize));
        accumulation.addReadPosi(2);
        next.fireRead(part);
        chunkSize = -1;
        return true;
    }

    private void resetState()
    {
        reqHead = null;
        bodyRead = 0;
        chunkSize = -1;
        state = ParseState.REQUEST_LINE;
        if (accumulation != null && accumulation.remainRead() == 0)
        {
            accumulation.free();
            accumulation = null;
        }
    }

    enum ParseState
    {
        REQUEST_LINE, REQUEST_HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
