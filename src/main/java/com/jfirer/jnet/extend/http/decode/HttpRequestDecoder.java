package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.extend.http.common.HttpDecoder;

import java.nio.charset.StandardCharsets;

public class HttpRequestDecoder extends HttpDecoder
{
    enum ParseState
    {
        REQUEST_LINE,
        REQUEST_HEADER,
        REQUEST_BODY
    }

    private int         lastCheck = -1;
    private ParseState  state     = ParseState.REQUEST_LINE;
    private HttpRequest decodeObject;

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
        boolean goToNextState = false;
        do
        {
            switch (state)
            {
                case REQUEST_LINE -> goToNextState = parseRequestLine();
                case REQUEST_HEADER -> goToNextState = parseRequestHeader();
                case REQUEST_BODY ->
                {
                    parseRequestBody(next);
                    return;
                }
            }
        }
        while (goToNextState);
    }

    private void parseRequestBody(ReadProcessorNode next)
    {
        if (decodeObject.getContentLength() != 0)
        {
            if (accumulation.remainRead() < decodeObject.getContentLength())
            {
                accumulation.capacityReadyFor(decodeObject.getContentLength());
                return;
            }
            else
            {
                decodeObject.setBody(accumulation);
            }
        }
        else
        {
            accumulation.free();
        }
        next.fireRead(decodeObject);
        accumulation = null;
        decodeObject = null;
        state = ParseState.REQUEST_LINE;
        lastCheck = -1;
    }

    private boolean parseRequestHeader()
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                lastCheck += 4;
                state = ParseState.REQUEST_BODY;
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
        lastCheck = lastCheck == -1 ? accumulation.getReadPosi() : lastCheck;
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
        findAllHeaders(decodeObject::addHeader);
        findContentType(decodeObject.getHeaders(), decodeObject::setContentType);
        findContentLength(decodeObject.getHeaders(), decodeObject::setContentLength);
    }

}
