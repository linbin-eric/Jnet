package com.jfirer.jnet.extend.http.decode;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse
{
    private Map<String, String> headers = new HashMap<>();
    private String              body;
    private String              contentType;

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
