package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.extend.websocket.dto.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WebSocketFrameDecoder extends AbstractDecoder {

    /**
     * true=服务端模式（要求MASK=1），false=客户端模式（要求MASK=0）
     */
    private final boolean serverMode;

    private DecodeState state = DecodeState.FRAME_HEADER;

    // 当前帧解析状态
    private boolean fin;
    private int opcode;
    private boolean masked;
    private long payloadLength;
    private byte[] maskingKey = new byte[4];
    private int extendedLengthBytes; // 0, 2, 或 8

    // 分片消息缓存
    private List<IoBuffer> fragmentBuffers = new ArrayList<>();
    private int fragmentOpcode = -1;

    public WebSocketFrameDecoder(boolean serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    protected void process0(ReadProcessorNode next) {
        boolean continueProcessing;
        do {
            continueProcessing = switch (state) {
                case FRAME_HEADER -> parseFrameHeader();
                case EXTENDED_LENGTH -> parseExtendedLength();
                case MASKING_KEY -> parseMaskingKey();
                case PAYLOAD -> parsePayload(next);
            };
        } while (continueProcessing);
    }

    private boolean parseFrameHeader() {
        if (accumulation == null || accumulation.remainRead() < 2) {
            return false;
        }

        byte b1 = accumulation.get();
        byte b2 = accumulation.get();

        fin = (b1 & 0x80) != 0;
        opcode = b1 & 0x0F;
        masked = (b2 & 0x80) != 0;
        payloadLength = b2 & 0x7F;

        // 校验MASK
        if (serverMode && !masked) {
            throw new IllegalStateException("服务端模式要求客户端发送的帧必须有掩码");
        }
        if (!serverMode && masked) {
            throw new IllegalStateException("客户端模式要求服务端发送的帧不能有掩码");
        }

        // 控制帧校验
        if (opcode >= 8) {
            if (!fin) {
                throw new IllegalStateException("控制帧不能被分片");
            }
            if (payloadLength > 125) {
                throw new IllegalStateException("控制帧payload不能超过125字节");
            }
        }

        // 确定扩展长度
        if (payloadLength == 126) {
            extendedLengthBytes = 2;
            state = DecodeState.EXTENDED_LENGTH;
        } else if (payloadLength == 127) {
            extendedLengthBytes = 8;
            state = DecodeState.EXTENDED_LENGTH;
        } else {
            extendedLengthBytes = 0;
            state = masked ? DecodeState.MASKING_KEY : DecodeState.PAYLOAD;
        }

        return true;
    }

    private boolean parseExtendedLength() {
        if (accumulation.remainRead() < extendedLengthBytes) {
            return false;
        }

        if (extendedLengthBytes == 2) {
            payloadLength = accumulation.getShort() & 0xFFFF;
        } else {
            payloadLength = accumulation.getLong();
        }

        state = masked ? DecodeState.MASKING_KEY : DecodeState.PAYLOAD;
        return true;
    }

    private boolean parseMaskingKey() {
        if (accumulation.remainRead() < 4) {
            return false;
        }

        accumulation.get(maskingKey);
        state = DecodeState.PAYLOAD;
        return true;
    }

    private boolean parsePayload(ReadProcessorNode next) {
        if (accumulation.remainRead() < payloadLength) {
            return false;
        }

        // 读取payload
        IoBuffer payload = null;
        if (payloadLength > 0) {
            payload = accumulation.slice((int) payloadLength);

            // 如果有掩码，进行解码
            if (masked) {
                unmask(payload);
            }
        }

        // 处理帧
        handleFrame(next, payload);

        // 重置状态准备解析下一帧
        resetParseState();

        return accumulation != null && accumulation.remainRead() > 0;
    }

    private void unmask(IoBuffer payload) {
        int readPosi = payload.getReadPosi();
        int length = payload.remainRead();
        for (int i = 0; i < length; i++) {
            byte original = payload.get(readPosi + i);
            byte decoded = (byte) (original ^ maskingKey[i % 4]);
            payload.put(decoded, readPosi + i);
        }
    }

    private void handleFrame(ReadProcessorNode next, IoBuffer payload) {
        // 处理控制帧
        if (opcode >= 8) {
            handleControlFrame(next, payload);
            return;
        }

        // 处理数据帧（分片组装）
        if (opcode != WebSocketFrame.OPCODE_CONTINUATION) {
            // 新消息的第一帧
            fragmentOpcode = opcode;
            fragmentBuffers.clear();
        }

        if (payload != null) {
            fragmentBuffers.add(payload);
        }

        if (fin) {
            // 消息完整，组装并输出
            WebSocketFrame frame = new WebSocketFrame();
            frame.setFin(true);
            frame.setOpcode(fragmentOpcode);
            frame.setLast(true);

            if (fragmentBuffers.size() == 1) {
                frame.setPayload(fragmentBuffers.get(0));
            } else if (fragmentBuffers.size() > 1) {
                // 合并所有分片
                frame.setPayload(mergeBuffers(next));
            }

            fragmentBuffers.clear();
            fragmentOpcode = -1;

            next.fireRead(frame);
        }
    }

    private IoBuffer mergeBuffers(ReadProcessorNode next) {
        int totalLength = 0;
        for (IoBuffer buf : fragmentBuffers) {
            totalLength += buf.remainRead();
        }

        IoBuffer merged = next.pipeline().allocator().allocate(totalLength);
        for (IoBuffer buf : fragmentBuffers) {
            merged.put(buf);
            buf.free();
        }
        return merged;
    }

    private void handleControlFrame(ReadProcessorNode next, IoBuffer payload) {
        WebSocketFrame frame = new WebSocketFrame();
        frame.setFin(true);
        frame.setOpcode(opcode);
        frame.setPayload(payload);

        switch (opcode) {
            case WebSocketFrame.OPCODE_PING -> {
                // 自动发送Pong响应
                WebSocketFrame pong = WebSocketFrame.createPong(payload);
                next.pipeline().fireWrite(pong);
                // 传递Ping帧给上层（payload已交给pong，创建新帧）
                WebSocketFrame pingNotify = new WebSocketFrame();
                pingNotify.setFin(true);
                pingNotify.setOpcode(WebSocketFrame.OPCODE_PING);
                next.fireRead(pingNotify);
            }
            case WebSocketFrame.OPCODE_PONG -> {
                // 直接传递给上层
                next.fireRead(frame);
            }
            case WebSocketFrame.OPCODE_CLOSE -> {
                // 解析关闭状态码和原因
                int closeCode = 1000;
                String closeReason = "";
                if (payload != null && payload.remainRead() >= 2) {
                    closeCode = payload.getShort() & 0xFFFF;
                    if (payload.remainRead() > 0) {
                        byte[] reasonBytes = new byte[payload.remainRead()];
                        payload.get(reasonBytes);
                        closeReason = new String(reasonBytes, StandardCharsets.UTF_8);
                    }
                }

                frame.setCloseCode(closeCode);
                frame.setCloseReason(closeReason);

                // 自动发送Close响应
                WebSocketFrame closeResp = WebSocketFrame.createClose(closeCode, closeReason);
                next.pipeline().fireWrite(closeResp);

                // 传递Close帧给上层
                next.fireRead(frame);

                // 关闭输入
                next.pipeline().shutdownInput();
            }
        }
    }

    private void resetParseState() {
        state = DecodeState.FRAME_HEADER;
        fin = false;
        opcode = 0;
        masked = false;
        payloadLength = 0;
        extendedLengthBytes = 0;

        if (accumulation != null && accumulation.remainRead() > 0) {
            accumulation.compact();
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next) {
        // 清理分片缓存
        for (IoBuffer buf : fragmentBuffers) {
            buf.free();
        }
        fragmentBuffers.clear();
        super.readFailed(e, next);
    }

    enum DecodeState {
        FRAME_HEADER,
        EXTENDED_LENGTH,
        MASKING_KEY,
        PAYLOAD
    }
}
