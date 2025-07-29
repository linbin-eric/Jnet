package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.extend.http.coder.ContentType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class HttpSendRequest
{
    private String              url;
    private String              path;
    private String              doMain;
    private int                 port;
    private String              contentType;
    private Map<String, String> headers = new HashMap<>();
    private IoBuffer            bodyBuffer;
    private String              bodyString;
    private String              method;

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public HttpSendRequest setBody(String body)
    {
        this.bodyString = body;
        return this;
    }

    public HttpSendRequest setBody(IoBuffer buffer)
    {
        this.bodyBuffer = buffer;
        return this;
    }

    public HttpSendRequest get()
    {
        method = "GET";
        return this;
    }

    public HttpSendRequest post()
    {
        method = "POST";
        return this;
    }

    public HttpSendRequest applicationJsonType()
    {
        contentType = ContentType.APPLICATION_JSON;
        return this;
    }

    public void close()
    {
        if (bodyBuffer != null)
        {
            bodyBuffer.free();
            bodyBuffer = null;
        }
        bodyString = null;
    }
}
