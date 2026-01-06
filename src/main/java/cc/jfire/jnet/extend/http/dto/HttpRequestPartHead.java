package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpRequestPartHead implements HttpRequestPart
{
    /**
     * 完整的请求头部内容，包含结束的 CRLF
     */
    protected IoBuffer            headBuffer;
    protected String              domain;
    protected int                 port          = 80;
    protected String              method;
    protected String              path;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    /**
     * 如果长度是非 0，意味着是固定长度的请求。
     * 如果是 0且 chunked 是 true，意味着是流式请求。
     * 如果是 0 且 chunked 是 false，意味着没有请求体。
     */
    protected long                 contentLength = 0;
    protected boolean             chunked       = false;
    protected boolean             last          = false;

    @Override
    public boolean isLast()
    {
        return last;
    }

    public void setLast(boolean last)
    {
        this.last = last;
    }

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public HttpRequestPartHead setUrl(String url)
    {
        HttpUrl parsed = HttpUrl.parse(url);
        setPath(parsed.path());
        setDomain(parsed.domain());
        setPort(parsed.port());
        // Host header：大小写不敏感替换，避免产生重复 Host
        String matchedKey = null;
        for (String key : headers.keySet())
        {
            if (key.equalsIgnoreCase("Host"))
            {
                matchedKey = key;
                break;
            }
        }
        if (matchedKey != null)
        {
            headers.remove(matchedKey);
        }
        headers.put("Host", parsed.hostHeader());
        // 关键：清空并释放 headBuffer，否则编码器会直接写出原始头部，同时避免内存泄漏
        IoBuffer old = getHeadBuffer();
        setHeadBuffer(null);
        if (old != null)
        {
            old.free();
        }
        return this;
    }

    @Override
    public void close()
    {
        if (headBuffer != null)
        {
            headBuffer.free();
            headBuffer = null;
        }
        method  = path = version = null;
        headers = null;
    }
}
