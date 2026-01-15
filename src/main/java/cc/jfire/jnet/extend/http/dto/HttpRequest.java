package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "body")
public class HttpRequest implements AutoCloseable
{
    protected HttpRequestPartHead head = new HttpRequestPartHead();
    protected IoBuffer            body;
    protected String              strBody;
    protected boolean             ssl  = false;

    public void close()
    {
        head.close();
        if (body != null)
        {
            body.free();
            body = null;
        }
    }

    public HttpRequest setUrl(String url)
    {
        HttpUrl parsed = HttpUrl.parse(url);
        this.ssl = parsed.ssl();
        head.setUrl(url);
        return this;
    }

    public HttpRequest setContentType(String contentType)
    {
        head.addHeader("Content-Type", contentType);
        return this;
    }

    public HttpRequest addHeader(String name, String value)
    {
        head.addHeader(name, value);
        return this;
    }

    public HttpRequest setMethod(String method)
    {
        head.setMethod(method);
        return this;
    }

    public HttpRequest get()
    {
        head.setMethod("GET");
        return this;
    }

    public HttpRequest post()
    {
        head.setMethod("POST");
        return this;
    }

    public HttpRequest delete()
    {
        head.setMethod("DELETE");
        return this;
    }

    public HttpRequest put()
    {
        head.setMethod("PUT");
        return this;
    }

    public HttpRequest setBody(String body)
    {
        this.strBody = body;
        return this;
    }

    public HttpRequest setBody(IoBuffer body)
    {
        this.body = body;
        return this;
    }

    public boolean isSsl()
    {
        return ssl;
    }
}
