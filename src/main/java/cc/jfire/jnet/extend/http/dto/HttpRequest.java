package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString(exclude = "body")
public class HttpRequest implements AutoCloseable
{
    protected String              url;
    protected String              domain;
    protected int                 port          = 80;
    protected String              method;
    protected String              path;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    protected long                contentLength = 0;
    protected String              contentType;
    protected IoBuffer            body;
    protected String              strBody;

    public void close()
    {
        if (body != null)
        {
            body.free();
            body = null;
        }
    }

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public HttpRequest setUrl(String url)
    {
        this.url = url;
        HttpUrl parsed = HttpUrl.parse(url);
        this.domain = parsed.domain();
        this.port   = parsed.port();
        this.path   = parsed.path();
        this.headers.put("Host", parsed.hostHeader());
        return this;
    }
}
