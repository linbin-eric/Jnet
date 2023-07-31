package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse
{
    private Map<String, String> headers = new HashMap<>();
    private String              body;
    private IoBuffer            bodyBuffer;
    private byte[]              bytes_body;
    private String              contentType;

    public void addHeader(String header, String value)
    {
        headers.put(header, value);
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public IoBuffer getBodyBuffer()
    {
        return bodyBuffer;
    }

    public void setBodyBuffer(IoBuffer bodyBuffer)
    {
        this.bodyBuffer = bodyBuffer;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public byte[] getBytes_body()
    {
        return bytes_body;
    }

    public void setBytes_body(byte[] bytes_body)
    {
        this.bytes_body = bytes_body;
    }
}
