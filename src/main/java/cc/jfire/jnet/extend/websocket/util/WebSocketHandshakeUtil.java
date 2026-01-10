package cc.jfire.jnet.extend.websocket.util;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class WebSocketHandshakeUtil {

    /**
     * WebSocket GUID，RFC 6455 规定的固定值
     */
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * 判断是否为 WebSocket 升级请求
     */
    public static boolean isWebSocketUpgrade(HttpRequestPartHead head) {
        if (head == null || head.getHeaders() == null) {
            return false;
        }
        Map<String, String> headers = head.getHeaders();

        // 检查必要的头部
        String upgrade = getHeaderIgnoreCase(headers, "Upgrade");
        String connection = getHeaderIgnoreCase(headers, "Connection");
        String secWebSocketKey = getHeaderIgnoreCase(headers, "Sec-WebSocket-Key");
        String secWebSocketVersion = getHeaderIgnoreCase(headers, "Sec-WebSocket-Version");

        return "websocket".equalsIgnoreCase(upgrade)
                && connection != null && connection.toLowerCase().contains("upgrade")
                && secWebSocketKey != null && !secWebSocketKey.isEmpty()
                && "13".equals(secWebSocketVersion);
    }

    /**
     * 获取 Sec-WebSocket-Key
     */
    public static String getSecWebSocketKey(HttpRequestPartHead head) {
        return getHeaderIgnoreCase(head.getHeaders(), "Sec-WebSocket-Key");
    }

    /**
     * 根据 RFC 6455 计算 Sec-WebSocket-Accept 值
     * Accept = Base64(SHA-1(Sec-WebSocket-Key + GUID))
     */
    public static String computeAcceptKey(String secWebSocketKey) {
        try {
            String combined = secWebSocketKey + WEBSOCKET_GUID;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(combined.getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * 构建 101 Switching Protocols 响应
     */
    public static IoBuffer buildUpgradeResponse(HttpRequestPartHead head, BufferAllocator allocator) {
        String secWebSocketKey = getSecWebSocketKey(head);
        String acceptKey = computeAcceptKey(secWebSocketKey);

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 101 Switching Protocols\r\n");
        sb.append("Upgrade: websocket\r\n");
        sb.append("Connection: Upgrade\r\n");
        sb.append("Sec-WebSocket-Accept: ").append(acceptKey).append("\r\n");
        sb.append("\r\n");

        byte[] responseBytes = sb.toString().getBytes(StandardCharsets.US_ASCII);
        IoBuffer buffer = allocator.allocate(responseBytes.length);
        buffer.put(responseBytes);
        return buffer;
    }

    /**
     * 忽略大小写获取 header 值
     */
    private static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
