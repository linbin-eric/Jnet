package cc.jfire.jnet.extend.http.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpResponsePartHead implements HttpResponsePart
{
    protected String              version;
    protected int                 statusCode;
    protected String              reasonPhrase;
    protected Map<String, String> headers       = new HashMap<>();
    /**
     * 如果长度是非 0，意味着是固定长度的响应。
     * 如果是 0 且 chunked 是 true，意味着是流式响应。
     * 如果是 0 且 chunked 是 false，意味着没有响应体。
     */
    protected int                 contentLength = 0;
    protected boolean             chunked       = false;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }
}
