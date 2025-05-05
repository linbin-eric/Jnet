package com.jfirer.jnet.extend.http.dto;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRespHead implements HttpRespPart
{
    @Setter
    @Getter
    private int                 responseCode     = 200;
    private Map<String, String> headers          = new HashMap<>();
    private boolean             hasContentLength = false;
    private boolean             hasContentType   = false;

    public HttpRespHead addHeader(String name, String value)
    {
        if (name.toLowerCase().startsWith("content-length"))
        {
            hasContentLength = true;
        }
        if (name.toLowerCase().startsWith("content-type"))
        {
            hasContentType = true;
        }
        headers.put(name, value);
        return this;
    }

    public boolean hasContentLength()
    {
        return hasContentLength;
    }

    public boolean hasContentType()
    {
        return hasContentType;
    }

    public void write(IoBuffer buffer)
    {
        buffer.put(("HTTP/1.1 " + responseCode + " OK\n").getBytes(StandardCharsets.US_ASCII));
        headers.forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
    }
}
