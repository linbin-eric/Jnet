package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Data
public class HttpResponse
{
    private String              version;
    private int                 statusCode;
    private String              reasonPhrase;
    private Map<String, String> headers = new HashMap<>();
    private IoBuffer            body;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public String getBodyAsString()
    {
        if (body == null || body.remainRead() == 0)
        {
            return "";
        }
        return StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
    }

    public void free()
    {
        if (body != null)
        {
            body.free();
            body = null;
        }
    }
}

