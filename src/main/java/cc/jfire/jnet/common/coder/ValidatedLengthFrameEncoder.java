package cc.jfire.jnet.common.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

/**
 * 安全增强型基于长度的报文编码器
 * <p>
 * 与 {@link ValidatedLengthFrameDecoder} 配套使用，将报文体编码为带有魔法值、长度和CRC16校验的完整报文。
 * <p>
 * 报文格式：
 * <pre>
 * +----------------+----------------+----------------+
 * |    魔法值       |    长度字段     |    CRC16       |
 * |   (4字节)       |   (4字节)       |   (2字节)       |
 * +----------------+----------------+----------------+
 * |                    报文体                         |
 * +--------------------------------------------------+
 * </pre>
 */
public class ValidatedLengthFrameEncoder implements WriteProcessor<IoBuffer>
{
    private final int             magic;
    private final BufferAllocator allocator;

    /**
     * 创建编码器
     *
     * @param magic     4字节魔法值，需与解码器配置一致
     * @param allocator 缓冲区分配器
     */
    public ValidatedLengthFrameEncoder(int magic, BufferAllocator allocator)
    {
        this.magic     = magic;
        this.allocator = allocator;
    }

    @Override
    public void write(IoBuffer body, WriteProcessorNode next)
    {
        int bodyLength  = body.remainRead();
        int totalLength = ValidatedLengthFrameDecoder.HEADER_SIZE + bodyLength;
        IoBuffer frame = allocator.allocate(totalLength);
        frame.putInt(magic);
        frame.putInt(bodyLength);
        short crc = calculateHeaderCrc(magic, bodyLength);
        frame.putShort(crc);
        frame.put(body);
        body.free();
        next.fireWrite(frame);
    }

    /**
     * 计算报文头的 CRC16 校验和
     *
     * @param magic  魔法值
     * @param length 报文体长度
     * @return CRC16 校验和
     */
    private short calculateHeaderCrc(int magic, int length)
    {
        byte[] header = new byte[8];
        header[0] = (byte) (magic >> 24);
        header[1] = (byte) (magic >> 16);
        header[2] = (byte) (magic >> 8);
        header[3] = (byte) magic;
        header[4] = (byte) (length >> 24);
        header[5] = (byte) (length >> 16);
        header[6] = (byte) (length >> 8);
        header[7] = (byte) length;
        return CRC16Util.crc16(header, 0, 8);
    }

    public int getMagic()
    {
        return magic;
    }
}
