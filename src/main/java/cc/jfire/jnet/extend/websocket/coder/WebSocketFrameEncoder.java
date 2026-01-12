package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.websocket.dto.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Slf4j
public class WebSocketFrameEncoder implements WriteProcessor<Object>
{
    private final BufferAllocator allocator;
    /**
     * true=客户端模式（发送时添加掩码），false=服务端模式（发送时不加掩码）
     */
    private final boolean         clientMode;
    private final SecureRandom    random = new SecureRandom();

    public WebSocketFrameEncoder(BufferAllocator allocator, boolean clientMode)
    {
        this.allocator  = allocator;
        this.clientMode = clientMode;
    }

    @Override
    public void write(Object obj, WriteProcessorNode next)
    {
        if (obj instanceof WebSocketFrame frame)
        {
            IoBuffer buffer = encodeFrame(frame);
            next.fireWrite(buffer);
        }
        else
        {
            next.fireWrite(obj);
        }
    }

    private IoBuffer encodeFrame(WebSocketFrame frame)
    {
        // 计算需要的缓冲区大小
        int      payloadLength = 0;
        IoBuffer payload       = frame.getPayload();
        // 处理Close帧的特殊payload
        byte[] closePayload = null;
        if (frame.getOpcode() == WebSocketFrame.OPCODE_CLOSE)
        {
            closePayload  = buildClosePayload(frame);
            payloadLength = closePayload.length;
        }
        else if (payload != null)
        {
            payloadLength = payload.remainRead();
        }
        int headerSize = 2;
        if (payloadLength > 125 && payloadLength <= 65535)
        {
            headerSize += 2;
        }
        else if (payloadLength > 65535)
        {
            headerSize += 8;
        }
        if (clientMode)
        {
            headerSize += 4; // masking key
        }
        IoBuffer buffer = allocator.allocate(headerSize + payloadLength);
        // 第1字节: FIN + RSV + Opcode
        byte b1 = (byte) ((frame.isFin() ? 0x80 : 0) | (frame.getOpcode() & 0x0F));
        buffer.put(b1);
        // 第2字节: MASK + Payload Length
        byte b2 = (byte) (clientMode ? 0x80 : 0);
        if (payloadLength <= 125)
        {
            b2 |= payloadLength;
            buffer.put(b2);
        }
        else if (payloadLength <= 65535)
        {
            b2 |= 126;
            buffer.put(b2);
            buffer.putShort((short) payloadLength);
        }
        else
        {
            b2 |= 127;
            buffer.put(b2);
            buffer.putLong(payloadLength);
        }
        // Masking Key (仅客户端模式)
        byte[] maskingKey = null;
        if (clientMode)
        {
            maskingKey = new byte[4];
            random.nextBytes(maskingKey);
            buffer.put(maskingKey);
        }
        // Payload
        if (closePayload != null)
        {
            if (clientMode)
            {
                // 掩码编码
                for (int i = 0; i < closePayload.length; i++)
                {
                    buffer.put((byte) (closePayload[i] ^ maskingKey[i % 4]));
                }
            }
            else
            {
                buffer.put(closePayload);
            }
        }
        else if (payload != null && payloadLength > 0)
        {
            if (clientMode)
            {
                // 掩码编码
                int readPosi = payload.getReadPosi();
                for (int i = 0; i < payloadLength; i++)
                {
                    byte original = payload.get(readPosi + i);
                    buffer.put((byte) (original ^ maskingKey[i % 4]));
                }
            }
            else
            {
                buffer.put(payload, payloadLength);
            }
        }
        return buffer;
    }

    private byte[] buildClosePayload(WebSocketFrame frame)
    {
        int    closeCode   = frame.getCloseCode();
        String closeReason = frame.getCloseReason();
        if (closeCode == 0 && (closeReason == null || closeReason.isEmpty()))
        {
            return new byte[0];
        }
        byte[] reasonBytes = (closeReason != null) ? closeReason.getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] result      = new byte[2 + reasonBytes.length];
        result[0] = (byte) ((closeCode >> 8) & 0xFF);
        result[1] = (byte) (closeCode & 0xFF);
        System.arraycopy(reasonBytes, 0, result, 2, reasonBytes.length);
        return result;
    }
}
