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
    protected String              method;
    protected String              url;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    protected int                 contentLength = 0;
    protected String              contentType;
    protected IoBuffer            body;

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
}
