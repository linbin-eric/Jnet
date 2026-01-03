package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpRequestPartHead implements HttpRequestPart
{
    protected IoBuffer            headBuffer;
    protected String              method;
    protected String              url;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    /**
     * 如果长度是非 0，意味着是固定长度的请求。
     * 如果是 0且 chunked 是 true，意味着是流式请求。
     * 如果是 0 且 chunked 是 false，意味着没有请求体。
     */
    protected int                 contentLength = 0;
    protected boolean             chunked       = false;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }
}
