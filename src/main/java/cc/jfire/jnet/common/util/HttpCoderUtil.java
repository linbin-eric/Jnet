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
    private static final byte[] NEW_LINE                 = "\r\n".getBytes(StandardCharsets.US_ASCII);
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
                buffer.put((entry.getKey() + ": ").getBytes(StandardCharsets.US_ASCII));
            }
            buffer.put(entry.getValue().getBytes(StandardCharsets.US_ASCII));
            buffer.put(NEW_LINE);
        }
        buffer.put(NEW_LINE);
    }

    public static void findAllHeaders(IoBuffer ioBuffer, BiConsumer<String, String> consumer)
    {
        String headerName = null, headerValue = null;
        while (ioBuffer.get(ioBuffer.getReadPosi()) != '\r' || ioBuffer.get(ioBuffer.getReadPosi() + 1) != '\n')
        {
            for (int i = ioBuffer.getReadPosi(); i < ioBuffer.getWritePosi(); i++)
            {
                if (ioBuffer.get(i) == ':')
                {
                    headerName = normalizeHeaderName(StandardCharsets.US_ASCII.decode(ioBuffer.readableByteBuffer(i)).toString());
                    ioBuffer.setReadPosi(i + 2);
                    break;
                }
            }
            for (int i = ioBuffer.getReadPosi(); i < ioBuffer.getWritePosi(); i++)
            {
                if (ioBuffer.get(i) == '\r')
                {
                    headerValue = StandardCharsets.US_ASCII.decode(ioBuffer.readableByteBuffer(i)).toString();
                    ioBuffer.setReadPosi(i + 2);
                    break;
                }
            }
            consumer.accept(headerName, headerValue);
        }
        ioBuffer.addReadPosi(2);
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
