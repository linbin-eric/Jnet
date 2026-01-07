package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class FullHttpResponse implements HttpResponsePart
{
    private static final byte[] NEWLINE = "\r\n".getBytes(StandardCharsets.US_ASCII);

    private String              version      = "HTTP/1.1";
    private int                 statusCode   = 200;
    private String              reasonPhrase = "OK";
    private Map<String, String> headers      = new HashMap<>();

    // 响应体，三种形式，优先级：bodyBuffer > bodyBytes > bodyText
    private IoBuffer bodyBuffer;
    private byte[]   bodyBytes;
    private String   bodyText;

    private boolean hasContentLength = false;
    private boolean hasContentType   = false;

    public FullHttpResponse addHeader(String name, String value)
    {
        String lowerName = name.toLowerCase();
        if (lowerName.equals("content-length"))
        {
            hasContentLength = true;
        }
        if (lowerName.equals("content-type"))
        {
            hasContentType = true;
        }
        headers.put(name, value);
        return this;
    }

    public void write(IoBuffer buffer)
    {
        helpSetContentLengthIfNeed();
        helpSetContentTypeIfNeed();
        // 编码响应行
        buffer.put((version + " " + statusCode + " " + reasonPhrase + "\r\n").getBytes(StandardCharsets.US_ASCII));
        // 编码 headers
        headers.forEach((name, value) -> buffer.put((name + ": " + value + "\r\n").getBytes(StandardCharsets.US_ASCII)));
        // 空行结束头部
        buffer.put(NEWLINE);
        // 编码 body
        writeBody(buffer);
    }

    private void helpSetContentLengthIfNeed()
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
            else if (bodyText != null)
            {
                addHeader("Content-Length", String.valueOf(bodyText.getBytes(StandardCharsets.UTF_8).length));
            }
            else
            {
                addHeader("Content-Length", "0");
            }
        }
    }

    private void helpSetContentTypeIfNeed()
    {
        if (!hasContentType)
        {
            addHeader("Content-Type", "application/json;charset=utf-8");
        }
    }

    private void writeBody(IoBuffer buffer)
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
        else if (bodyText != null)
        {
            buffer.put(bodyText.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void free()
    {
        if (bodyBuffer != null)
        {
            bodyBuffer.free();
            bodyBuffer = null;
        }
    }

    @Override
    public boolean isLast()
    {
        return true; // 完整响应总是最后一个部分
    }
}
