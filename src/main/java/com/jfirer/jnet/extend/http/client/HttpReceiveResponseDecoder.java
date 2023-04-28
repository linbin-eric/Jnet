package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.decoder.AbstractDecoder;

import java.nio.charset.StandardCharsets;

public class HttpReceiveResponseDecoder extends AbstractDecoder
{
    private              HttpReceiveResponse receiveResponse;
    private              ParseState          state     = ParseState.RESPONSE_LINE;
    private              int                 lastCheck = -1;
    private              byte[]              httpCode  = new byte[3];
    private static final byte                re        = "\r".getBytes(StandardCharsets.US_ASCII)[0];
    private static final byte                nl        = "\n".getBytes(StandardCharsets.US_ASCII)[0];

    public HttpReceiveResponseDecoder()
    {
        super(HttpClient.ALLOCATOR);
    }

    enum ParseState
    {
        RESPONSE_LINE,
        HEADER,
        BODY;
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (receiveResponse == null)
        {
            receiveResponse = new HttpReceiveResponse();
        }
        switch (state)
        {
            case RESPONSE_LINE ->
            {
                lastCheck = lastCheck == -1 ? accumulation.getReadPosi() : lastCheck;
                for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
                {
                    System.out.print((char) accumulation.get(lastCheck));
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
                    process0(next);
                }
            }
            case HEADER ->
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
                    receiveResponse.getHeaders().entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("content-length")).findFirst().ifPresent(v -> receiveResponse.setContentLength(Integer.parseInt(v.getValue())));
                    receiveResponse.getHeaders().entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("content-type")).findFirst().ifPresent(v -> receiveResponse.setContentType(v.getValue()));
                    accumulation.addReadPosi(2);
                    accumulation.capacityReadyFor(receiveResponse.getContentLength());
                    process0(next);
                }
            }
            case BODY ->
            {
                if (accumulation.remainRead() >= receiveResponse.getContentLength())
                {
                    receiveResponse.setBody(accumulation.slice(receiveResponse.getContentLength()));
                    next.fireRead(receiveResponse);
                    receiveResponse = null;
                    lastCheck = -1;
                    state = ParseState.RESPONSE_LINE;
                    compactIfNeed();
                    process0(next);
                }
                else
                {
                    ;
                }
            }
        }
    }
}
