package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class HttpResponse
{
    private HttpResponsePartHead head             = new HttpResponsePartHead();
    // 响应体，两种形式，优先级：bodyBuffer > bodyBytes
    private IoBuffer             bodyBuffer;
    private byte[]               bodyBytes;
    private boolean              hasContentLength = false;
    private boolean              hasContentType   = false;

    // 初始化 head 的默认值
    {
        head.setVersion("HTTP/1.1");
        head.setStatusCode(200);
        head.setReasonPhrase("OK");
    }

    public HttpResponse addHeader(String name, String value)
    {
        // 使用 HttpDecodeUtil 进行标准化
        String standardName = HttpDecodeUtil.normalizeHeaderName(name);
        String lowerName    = name.toLowerCase();
        if (lowerName.equals("content-length"))
        {
            hasContentLength = true;
        }
        else if (lowerName.equals("content-type"))
        {
            hasContentType = true;
        }
        head.addHeader(standardName, value);
        return this;
    }

    public void helpSetContentLengthIfNeed()
    {
        if (!hasContentLength)
        {
            if (bodyBuffer != null && bodyBuffer.remainRead() > 0)
            {
                addHeader("Content-Length", String.valueOf(bodyBuffer.remainRead()));
            }
            else if (bodyBytes != null)
            {
                addHeader("Content-Length", String.valueOf(bodyBytes.length));
            }
            else
            {
                addHeader("Content-Length", "0");
            }
        }
    }

    public void helpSetContentTypeIfNeed()
    {
        if (!hasContentType)
        {
            addHeader("Content-Type", "application/json;charset=utf-8");
        }
    }

    public void setBodyText(String bodyText)
    {
        if (bodyText != null)
        {
            this.bodyBytes = bodyText.getBytes(StandardCharsets.UTF_8);
        }
        else
        {
            this.bodyBytes = null;
        }
    }

    public void writeBody(IoBuffer buffer)
    {
        if (bodyBuffer != null)
        {
            buffer.put(bodyBuffer);
            bodyBuffer.free();
            bodyBuffer = null;
        }
        else if (bodyBytes != null)
        {
            buffer.put(bodyBytes);
        }
    }

    public void free()
    {
        head.free();
        if (bodyBuffer != null)
        {
            bodyBuffer.free();
            bodyBuffer = null;
        }
    }
}
