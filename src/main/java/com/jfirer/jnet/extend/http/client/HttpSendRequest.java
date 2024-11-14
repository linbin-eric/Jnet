package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.extend.http.decode.ContentType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
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
    private IoBuffer            body;
    private String              method;

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public HttpSendRequest setBody(String body)
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        this.body = HttpClient.ALLOCATOR.ioBuffer(bytes.length);
        this.body.put(bytes);
        return this;
    }

    public HttpSendRequest setBody(IoBuffer buffer)
    {
        this.body = buffer;
        return this;
    }

    public HttpSendRequest getRequest()
    {
        method = "GET";
        return this;
    }

    public HttpSendRequest postRequest()
    {
        method = "POST";
        return this;
    }

    public HttpSendRequest applicationJsonType()
    {
        contentType = ContentType.APPLICATION_JSON;
        return this;
    }

    public void freeBodyBuffer()
    {
        if (body != null)
        {
            body.free();
            body = null;
        }
    }
}
