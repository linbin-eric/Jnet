package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpRequestChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static cc.jfire.jnet.common.util.HttpCoderUtil.writeHeaderValue;

public class HttpRequestPartEncoder implements WriteProcessor<Object>
{
    private static final byte[] NEW_LINE                 = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final String CONTENT_LENGTH_HEADER    = "Content-Length";
    private static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (data instanceof HttpRequest)
        {
            encodeHttpRequest((HttpRequest) data, next);
        }
        else if (data instanceof HttpRequestPartHead)
        {
            encodeHttpRequestPartHead((HttpRequestPartHead) data, next);
        }
        else if (data instanceof HttpRequestFixLengthBodyPart)
        {
            encodeFixLengthBody((HttpRequestFixLengthBodyPart) data, next);
        }
        else if (data instanceof HttpRequestChunkedBodyPart)
        {
            encodeChunkedBody((HttpRequestChunkedBodyPart) data, next);
        }
        else
        {
            next.fireWrite(data);
        }
    }

    /**
     * 检查 headers 中是否存在指定名称的 header（header name 已标准化）
     */
    private boolean containsHeader(Map<String, String> headers, String headerName)
    {
        return headers.containsKey(headerName);
    }

    private void removeHeader(Map<String, String> headers, String headerName)
    {
        headers.remove(headerName);
    }

    private void encodeHttpRequest(HttpRequest request, WriteProcessorNode next)
    {
        IoBuffer            buffer = next.pipeline().allocator().allocate(1024);
        HttpRequestPartHead head   = request.getHead();
        // 写入请求行
        String requestLine = STR.format("{} {} {}\r\n", head.getMethod(), head.getPath(), head.getVersion() != null ? head.getVersion() : "HTTP/1.1");
        buffer.put(requestLine.getBytes(StandardCharsets.US_ASCII));
        // 计算 body 长度
        int    contentLength = 0;
        byte[] strBodyBytes  = null;
        if (request.getBody() != null)
        {
            contentLength = request.getBody().remainRead();
        }
        else if (request.getStrBody() != null)
        {
            strBodyBytes  = request.getStrBody().getBytes(StandardCharsets.UTF_8);
            contentLength = strBodyBytes.length;
        }
        boolean chunked = head.isChunked();
        if (chunked)
        {
            // 避免产生 CL+TE 的歧义（以及潜在的请求走私风险）
            head.getHeaders().remove(CONTENT_LENGTH_HEADER);
            head.getHeaders().put(TRANSFER_ENCODING_HEADER, "chunked");
        }
        else
        {
            // 检查并补充 Content-Length（header name 已标准化）
            if (!containsHeader(head.getHeaders(), CONTENT_LENGTH_HEADER))
            {
                head.getHeaders().put(CONTENT_LENGTH_HEADER, String.valueOf(contentLength));
            }
        }
        // 写入 headers
        writeHeaderValue(head.getHeaders(), buffer);
        if (chunked)
        {
            // 以单个 chunk + 终止 chunk 的形式写出，保证协议正确
            if (contentLength > 0)
            {
                buffer.put(Integer.toHexString(contentLength).getBytes(StandardCharsets.US_ASCII));
                buffer.put(NEW_LINE);
                if (request.getBody() != null)
                {
                    buffer.put(request.getBody());
                }
                else
                {
                    buffer.put(strBodyBytes);
                }
                buffer.put(NEW_LINE);
            }
            buffer.put("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        else
        {
            // 写入 body
            if (request.getBody() != null)
            {
                buffer.put(request.getBody());
            }
            else if (strBodyBytes != null && strBodyBytes.length > 0)
            {
                buffer.put(strBodyBytes);
            }
        }
        // 避免 body 被二次 free（HttpRequest.close() 也会释放）
        request.close();
        next.fireWrite(buffer);
    }

    private void encodeHttpRequestPartHead(HttpRequestPartHead head, WriteProcessorNode next)
    {
        if (head.getHeadBuffer() != null)
        {
            // 直接写出 headBuffer
            next.fireWrite(head.getHeadBuffer());
        }
        else
        {
            // 按标准格式编码
            IoBuffer buffer      = next.pipeline().allocator().allocate(1024);
            String   requestLine = STR.format("{} {} {}\r\n", head.getMethod(), head.getPath(), head.getVersion() != null ? head.getVersion() : "HTTP/1.1");
            buffer.put(requestLine.getBytes(StandardCharsets.US_ASCII));
            writeHeaderValue(head.getHeaders(), buffer);
            next.fireWrite(buffer);
        }
    }

    private void encodeFixLengthBody(HttpRequestFixLengthBodyPart body, WriteProcessorNode next)
    {
        if (body.getPart() != null)
        {
            next.fireWrite(body.getPart());
        }
    }

    private void encodeChunkedBody(HttpRequestChunkedBodyPart body, WriteProcessorNode next)
    {
        if (body.getPart() != null)
        {
            next.fireWrite(body.getPart());
        }
    }
}
