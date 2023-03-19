package com.jfirer.jnet.extend.http.decode;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse
{
    private Map<String, String> headers = new HashMap<>();
    private String              body;
    private byte[]              bodyStream;
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

    public byte[] getBodyStream()
    {
        return bodyStream;
    }

    public void setBodyStream(byte[] bodyStream)
    {
        this.bodyStream = bodyStream;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
}
