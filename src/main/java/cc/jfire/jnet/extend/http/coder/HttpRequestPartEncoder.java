package cc.jfire.jnet.extend.http.coder;

import cc.jfire.baseutil.STR;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
     * 构建请求行中的路径部分
     * 子类可重写此方法以实现自定义路径格式（如代理模式下的完整 URL）
     *
     * @param head 请求头
     * @return 请求行中的路径部分
     */
    protected String buildPath(HttpRequestPartHead head)
    {
        return head.getPath();
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
        String requestLine = STR.format("{} {} {}\r\n", head.getMethod(), buildPath(head), head.getVersion() != null ? head.getVersion() : "HTTP/1.1");
        buffer.put(requestLine.getBytes(StandardCharsets.US_ASCII));
        // multipart/form-data 编码
        if (request.isMultipart())
        {
            encodeMultipartRequest(request, head, buffer, next);
            return;
        }
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

    protected void encodeHttpRequestPartHead(HttpRequestPartHead head, WriteProcessorNode next)
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
            String   requestLine = STR.format("{} {} {}\r\n", head.getMethod(), buildPath(head), head.getVersion() != null ? head.getVersion() : "HTTP/1.1");
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

    private void encodeMultipartRequest(HttpRequest request, HttpRequestPartHead head, IoBuffer buffer, WriteProcessorNode next)
    {
        String boundary  = "----JNetBoundary" + Long.toHexString(System.nanoTime());
        byte[] bodyBytes = buildMultipartBody(request.getMultipartParts(), boundary);
        // 移除可能存在的 Transfer-Encoding 避免 CL+TE 歧义
        head.getHeaders().remove(TRANSFER_ENCODING_HEADER);
        head.getHeaders().put("Content-Type", "multipart/form-data; boundary=" + boundary);
        head.getHeaders().put(CONTENT_LENGTH_HEADER, String.valueOf(bodyBytes.length));
        writeHeaderValue(head.getHeaders(), buffer);
        buffer.put(bodyBytes);
        request.close();
        next.fireWrite(buffer);
    }

    private byte[] buildMultipartBody(List<MultipartPart> parts, String boundary)
    {
        byte[] crlf            = "\r\n".getBytes(StandardCharsets.US_ASCII);
        byte[] boundaryLine    = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        byte[] endBoundaryLine = ("--" + boundary + "--").getBytes(StandardCharsets.US_ASCII);
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();)
        {
            for (MultipartPart part : parts)
            {
                baos.write(boundaryLine);
                baos.write(crlf);
                StringBuilder disposition = new StringBuilder("Content-Disposition: form-data; name=\"");
                disposition.append(part.getName()).append("\"");
                if (part.isFile())
                {
                    disposition.append("; filename=\"").append(part.getFilename()).append("\"");
                }
                baos.write(disposition.toString().getBytes(StandardCharsets.UTF_8));
                baos.write(crlf);
                if (part.isFile() && part.getContentType() != null)
                {
                    baos.write(("Content-Type: " + part.getContentType()).getBytes(StandardCharsets.US_ASCII));
                    baos.write(crlf);
                }
                baos.write(crlf);
                baos.write(part.getContent());
                baos.write(crlf);
            }
            baos.write(endBoundaryLine);
            baos.write(crlf);
            return baos.toByteArray();
        }
        catch (java.io.IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
