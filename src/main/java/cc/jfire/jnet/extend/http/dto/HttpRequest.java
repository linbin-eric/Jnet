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
        head.setUrl(url);
        return this;
    }

    public void setContentType(String contentType)
    {
        head.addHeader("Content-Type", contentType);
    }
}
