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
    protected int                 port    = 80;
    protected String              method;
    protected String              path;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    protected int                 contentLength = 0;
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
        int index = 0;
        int domainStart = 0;
        boolean isHttps = false;
        if (url.startsWith("http://"))
        {
            index = url.indexOf("/", 8);
            domainStart = 7;
        }
        else if (url.startsWith("https://"))
        {
            index = url.indexOf("/", 9);
            domainStart = 8;
            isHttps = true;
        }
        if (index == -1)
        {
            index = url.length();
        }
        int portStart = url.indexOf(':', domainStart);
        // 如果 portStart 大于 index，说明冒号在 path 中，不是端口分隔符
        if (portStart > index)
        {
            portStart = -1;
        }
        this.path = index == url.length() ? "/" : url.substring(index);
        this.port = portStart == -1 ? (isHttps ? 443 : 80) : Integer.parseInt(url.substring(portStart + 1, index));
        this.domain = portStart == -1 ? url.substring(domainStart, index) : url.substring(domainStart, portStart);
        // 构建 Host header
        String host = portStart == -1 ? this.domain : url.substring(domainStart, index);
        this.headers.put("Host", host);
        return this;
    }
}
