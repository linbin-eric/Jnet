package com.jfirer.jnet.extend.http.decode;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest
{
    private String              method;
    private String              url;
    private String              version;
    private Map<String, String> headers = new HashMap<>();
    private int                 contentLength;
    private String              body;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }

    public String getHeader(String header)
    {
        return headers.get(header);
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public String getBody()
    {
        return body;
    }

    @Override
    public String toString()
    {
        return "HttpRequest{" + "method='" + method + '\'' + ", url='" + url + '\'' + ", version='" + version + '\'' + ", headers=" + headers + ", contentLength=" + contentLength + ", body='" + body + '\'' + '}';
    }
}
