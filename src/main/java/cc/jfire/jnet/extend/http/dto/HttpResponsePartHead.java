package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
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
    /**
     * 代表该头部片段（响应行 + headers + CRLFCRLF）的原始字节；用于在被丢弃/超时/流式消费后释放。
     */
    protected IoBuffer            part;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    @Override
    public void free()
    {
        if (part != null)
        {
            part.free();
            part = null;
        }
    }
}
