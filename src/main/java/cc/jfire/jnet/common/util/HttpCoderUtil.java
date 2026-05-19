package cc.jfire.jnet.common.util;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.reverse.proxy.ContentTypeDist;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HttpCoderUtil
{
    private static final byte[]              NEW_LINE = "\r\n".getBytes(StandardCharsets.US_ASCII);
    private static final Map<String, String> STANDARD_HEADERS;
    private static final Map<String, byte[]> HEADER_KEY_BYTES_CACHE;

    static
    {
        STANDARD_HEADERS = new HashMap<>();
        // 通用头
        STANDARD_HEADERS.put("host", "Host");
        STANDARD_HEADERS.put("connection", "Connection");
        STANDARD_HEADERS.put("keep-alive", "Keep-Alive");
        STANDARD_HEADERS.put("cache-control", "Cache-Control");
        STANDARD_HEADERS.put("date", "Date");
        STANDARD_HEADERS.put("pragma", "Pragma");
        STANDARD_HEADERS.put("via", "Via");
        STANDARD_HEADERS.put("warning", "Warning");
        STANDARD_HEADERS.put("upgrade", "Upgrade");
        // 请求头
        STANDARD_HEADERS.put("accept", "Accept");
        STANDARD_HEADERS.put("accept-charset", "Accept-Charset");
        STANDARD_HEADERS.put("accept-encoding", "Accept-Encoding");
        STANDARD_HEADERS.put("accept-language", "Accept-Language");
        STANDARD_HEADERS.put("authorization", "Authorization");
        STANDARD_HEADERS.put("cookie", "Cookie");
        STANDARD_HEADERS.put("expect", "Expect");
        STANDARD_HEADERS.put("from", "From");
        STANDARD_HEADERS.put("if-match", "If-Match");
        STANDARD_HEADERS.put("if-modified-since", "If-Modified-Since");
        STANDARD_HEADERS.put("if-none-match", "If-None-Match");
        STANDARD_HEADERS.put("if-range", "If-Range");
        STANDARD_HEADERS.put("if-unmodified-since", "If-Unmodified-Since");
        STANDARD_HEADERS.put("max-forwards", "Max-Forwards");
        STANDARD_HEADERS.put("proxy-authorization", "Proxy-Authorization");
        STANDARD_HEADERS.put("range", "Range");
        STANDARD_HEADERS.put("referer", "Referer");
        STANDARD_HEADERS.put("te", "TE");
        STANDARD_HEADERS.put("user-agent", "User-Agent");
        // 实体头
        STANDARD_HEADERS.put("content-encoding", "Content-Encoding");
        STANDARD_HEADERS.put("content-language", "Content-Language");
        STANDARD_HEADERS.put("content-length", "Content-Length");
        STANDARD_HEADERS.put("content-location", "Content-Location");
        STANDARD_HEADERS.put("content-md5", "Content-MD5");
        STANDARD_HEADERS.put("content-range", "Content-Range");
        STANDARD_HEADERS.put("content-type", "Content-Type");
        STANDARD_HEADERS.put("expires", "Expires");
        STANDARD_HEADERS.put("last-modified", "Last-Modified");
        STANDARD_HEADERS.put("transfer-encoding", "Transfer-Encoding");
        // WebDAV 相关
        STANDARD_HEADERS.put("depth", "Depth");
        STANDARD_HEADERS.put("destination", "Destination");
        STANDARD_HEADERS.put("if", "If");
        STANDARD_HEADERS.put("lock-token", "Lock-Token");
        STANDARD_HEADERS.put("overwrite", "Overwrite");
        STANDARD_HEADERS.put("timeout", "Timeout");
        // CORS 相关
        STANDARD_HEADERS.put("origin", "Origin");
        STANDARD_HEADERS.put("access-control-request-method", "Access-Control-Request-Method");
        STANDARD_HEADERS.put("access-control-request-headers", "Access-Control-Request-Headers");
        // 其他常见
        STANDARD_HEADERS.put("x-forwarded-for", "X-Forwarded-For");
        STANDARD_HEADERS.put("x-forwarded-host", "X-Forwarded-Host");
        STANDARD_HEADERS.put("x-forwarded-proto", "X-Forwarded-Proto");
        STANDARD_HEADERS.put("x-real-ip", "X-Real-IP");
        STANDARD_HEADERS.put("x-requested-with", "X-Requested-With");
        // 构建 header key 字节数组缓存
        HEADER_KEY_BYTES_CACHE = new HashMap<>();
        for (String standardName : STANDARD_HEADERS.values())
        {
            HEADER_KEY_BYTES_CACHE.put(standardName, (standardName + ": ").getBytes(StandardCharsets.US_ASCII));
        }
    }

    public static String normalizeHeaderName(String name)
    {
        String normalized = STANDARD_HEADERS.get(name.toLowerCase());
        return normalized != null ? normalized : name;
    }

    public static byte[] getHeaderKeyBytes(String headerName)
    {
        return HEADER_KEY_BYTES_CACHE.get(headerName);
    }

    public static void writeHeaderValue(Map<String, String> map, IoBuffer buffer)
    {
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            byte[] keyBytes = HttpCoderUtil.getHeaderKeyBytes(entry.getKey());
            if (keyBytes != null)
            {
                buffer.put(keyBytes);
            }
            else
            {
                buffer.put((entry.getKey() + ": ").getBytes(StandardCharsets.UTF_8));
            }
            buffer.put(entry.getValue().getBytes(StandardCharsets.UTF_8));
            buffer.put(NEW_LINE);
        }
        buffer.put(NEW_LINE);
    }

    /**
     * AI 生成：按 CRLF 逐行解析 HTTP header，保证格式错误时受控失败且读指针持续推进。
     */
    public static void findAllHeaders(IoBuffer ioBuffer, BiConsumer<String, String> consumer)
    {
        while (true)
        {
            int lineStart = ioBuffer.getReadPosi();
            int writePosi = ioBuffer.getWritePosi();
            if (writePosi - lineStart < 2)
            {
                throw invalidHeader("incomplete header block");
            }
            if (ioBuffer.get(lineStart) == '\r')
            {
                if (ioBuffer.get(lineStart + 1) == '\n')
                {
                    ioBuffer.setReadPosi(lineStart + 2);
                    return;
                }
                throw invalidHeader("CR must be followed by LF");
            }

            int lineEnd = findCrlf(ioBuffer, lineStart, writePosi);
            if (lineEnd == -1)
            {
                throw invalidHeader("header line must end with CRLF");
            }
            int colon = findByte(ioBuffer, lineStart, lineEnd, (byte) ':');
            if (colon == -1)
            {
                throw invalidHeader("header line missing ':'");
            }

            String headerName = decodeRange(ioBuffer, lineStart, colon).trim();
            if (headerName.isEmpty())
            {
                throw invalidHeader("header name is empty");
            }

            int valueStart = colon + 1;
            while (valueStart < lineEnd)
            {
                byte b = ioBuffer.get(valueStart);
                if (b != ' ' && b != '\t')
                {
                    break;
                }
                valueStart++;
            }
            int valueEnd = lineEnd;
            while (valueEnd > valueStart)
            {
                byte b = ioBuffer.get(valueEnd - 1);
                if (b != ' ' && b != '\t')
                {
                    break;
                }
                valueEnd--;
            }

            String headerValue = decodeRange(ioBuffer, valueStart, valueEnd);
            ioBuffer.setReadPosi(lineEnd + 2);
            if (ioBuffer.getReadPosi() <= lineStart)
            {
                throw invalidHeader("read position did not advance");
            }
            consumer.accept(normalizeHeaderName(headerName), headerValue);
        }
    }

    /**
     * AI 生成：在限定范围内查找 CRLF，并把孤立 CR 识别为非法 header。
     */
    private static int findCrlf(IoBuffer ioBuffer, int start, int end)
    {
        for (int i = start; i + 1 < end; i++)
        {
            if (ioBuffer.get(i) == '\r')
            {
                if (ioBuffer.get(i + 1) == '\n')
                {
                    return i;
                }
                throw invalidHeader("CR must be followed by LF");
            }
        }
        return -1;
    }

    /**
     * AI 生成：在限定范围内查找单个目标字节，避免扫描越过当前 header 行。
     */
    private static int findByte(IoBuffer ioBuffer, int start, int end, byte target)
    {
        for (int i = start; i < end; i++)
        {
            if (ioBuffer.get(i) == target)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * AI 生成：临时移动读指针解码指定区间，解码后恢复原读指针。
     */
    private static String decodeRange(IoBuffer ioBuffer, int start, int end)
    {
        int oldReadPosi = ioBuffer.getReadPosi();
        ioBuffer.setReadPosi(start);
        String value = StandardCharsets.UTF_8.decode(ioBuffer.readableByteBuffer(end)).toString();
        ioBuffer.setReadPosi(oldReadPosi);
        return value;
    }

    /**
     * AI 生成：统一构造 header 解析异常，便于调用方识别协议格式错误。
     */
    private static IllegalArgumentException invalidHeader(String reason)
    {
        return new IllegalArgumentException("Invalid HTTP header: " + reason);
    }

    public static void findContentLength(Map<String, String> headers, Consumer<Long> contentLengthConsumer)
    {
        String value = headers.get("Content-Length");
        if (value != null)
        {
            contentLengthConsumer.accept(Long.valueOf(value));
        }
    }

    public static String pureUrl(String url)
    {
        int index = url.indexOf("#");
        if (index != -1)
        {
            url = url.substring(0, index);
        }
        index = url.indexOf("?");
        if (index != -1)
        {
            url = url.substring(0, index);
        }
        return url;
    }

    public static String findContentType(String url)
    {
        String contentType;
        int    i = url.lastIndexOf(".");
        if (i == -1)
        {
            contentType = "text/html";
        }
        else
        {
            contentType = ContentTypeDist.getOrDefault(url.substring(i), "text/html");
        }
        return contentType;
    }
}
