package cc.jfire.jnet.extend.websocket.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class WebSocketFrame {
    // 操作码常量
    public static final int OPCODE_CONTINUATION = 0;
    public static final int OPCODE_TEXT = 1;
    public static final int OPCODE_BINARY = 2;
    public static final int OPCODE_CLOSE = 8;
    public static final int OPCODE_PING = 9;
    public static final int OPCODE_PONG = 10;

    /**
     * 是否为消息的最后一帧
     */
    private boolean fin = true;

    /**
     * 操作码
     */
    private int opcode;

    /**
     * payload是否被掩码
     */
    private boolean masked;

    /**
     * 实际数据（已解码）
     */
    private IoBuffer payload;

    /**
     * 关闭状态码（仅Close帧使用）
     */
    private int closeCode;

    /**
     * 关闭原因（仅Close帧使用）
     */
    private String closeReason;

    /**
     * 是否为消息流的最后一部分
     */
    private boolean last = true;

    /**
     * 判断是否为控制帧
     */
    public boolean isControlFrame() {
        return opcode >= 8;
    }

    /**
     * 释放payload资源
     */
    public void free() {
        if (payload != null) {
            payload.free();
            payload = null;
        }
    }

    /**
     * 创建Pong响应帧
     */
    public static WebSocketFrame createPong(IoBuffer payload) {
        WebSocketFrame frame = new WebSocketFrame();
        frame.setFin(true);
        frame.setOpcode(OPCODE_PONG);
        frame.setPayload(payload);
        return frame;
    }

    /**
     * 创建Close响应帧
     */
    public static WebSocketFrame createClose(int code, String reason) {
        WebSocketFrame frame = new WebSocketFrame();
        frame.setFin(true);
        frame.setOpcode(OPCODE_CLOSE);
        frame.setCloseCode(code);
        frame.setCloseReason(reason);
        return frame;
    }
}
