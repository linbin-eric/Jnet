package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRequestDecoder extends AbstractDecoder
{
    private int         lastCheck      = -1;
    private ParseState  state          = ParseState.REQUEST_LINE;
    private HttpRequest decodeObject;
    private int         firstByteIndex = -1;
    //line的结束坐标，在/r/n后
    private int         lineEndIndex   = -1;
    //header的结束坐标，在/r/n后
    private int         headerEndIndex = -1;
    private String      remote;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (decodeObject == null)
        {
            decodeObject = new HttpRequest();
        }
        boolean goToNextState = false;
        do
        {
            switch (state)
            {
                case REQUEST_LINE -> goToNextState = parseRequestLine();
                case REQUEST_HEADER -> goToNextState = parseRequestHeader();
                case REQUEST_BODY -> goToNextState = parseRequestBody(next);
            }
        } while (goToNextState);
    }

    private boolean parseRequestBody(ReadProcessorNode next)
    {
        if (decodeObject.getContentLength() != 0)
        {
            if (accumulation.remainRead() < decodeObject.getContentLength())
            {
                accumulation.capacityReadyFor(decodeObject.getContentLength());
                return false;
            }
            else
            {
                decodeObject.setBody(accumulation.slice(decodeObject.getContentLength()));
            }
        }
        accumulation.setReadPosi(firstByteIndex);
        decodeObject.setLineLength(lineEndIndex - firstByteIndex);
        decodeObject.setHeaderLength(headerEndIndex - lineEndIndex);
        decodeObject.setBodyLength(accumulation.remainRead());
        decodeObject.setWholeRequest(accumulation.slice(accumulation.remainRead()));
        next.fireRead(decodeObject);
        decodeObject   = null;
        firstByteIndex = lineEndIndex = headerEndIndex = -1;
        state          = ParseState.REQUEST_LINE;
        if (accumulation.remainRead() != 0)
        {
            throw new IllegalStateException();
        }
        accumulation.free();
        accumulation = null;
        lastCheck    = -1;
        return false;
    }

    private boolean parseRequestHeader()
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck += 4;
                headerEndIndex = lastCheck;
                state          = ParseState.REQUEST_BODY;
                break;
            }
        }
        if (state == ParseState.REQUEST_BODY)
        {
            decodeHeaders();
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean parseRequestLine()
    {
        if (lastCheck == -1)
        {
            firstByteIndex = lastCheck = accumulation.getReadPosi();
        }
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
            {
                lastCheck += 2;
                lineEndIndex = lastCheck;
                state        = ParseState.REQUEST_HEADER;
                break;
            }
        }
        if (state == ParseState.REQUEST_HEADER)
        {
            decodeRequestLine();
            return true;
        }
        else
        {
            return false;
        }
    }

    private void decodeRequestLine()
    {
        for (int i = 0; i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                decodeObject.setMethod(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                decodeObject.setUrl(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        accumulation.setReadPosi(lastCheck);
    }

    private void decodeHeaders()
    {
        HttpDecodeUtil.findAllHeaders(accumulation, decodeObject::addHeader);
        HttpDecodeUtil.findContentType(decodeObject.getHeaders(), decodeObject::setContentType);
        HttpDecodeUtil.findContentLength(decodeObject.getHeaders(), decodeObject::setContentLength);
    }

    enum ParseState
    {
        REQUEST_LINE, REQUEST_HEADER, REQUEST_BODY
    }
}
