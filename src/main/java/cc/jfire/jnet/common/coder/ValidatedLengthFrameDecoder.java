package cc.jfire.jnet.common.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.exception.TooLongException;

/**
 * 安全增强型基于长度的报文解码器
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
 * <p>
 * 验证机制：
 * <ul>
 *   <li>魔法值检查：验证前4字节是否与预设值匹配</li>
 *   <li>长度范围校验：验证长度是否在 [minLength, maxLength] 范围内</li>
 *   <li>CRC16校验：对魔法值和长度字段计算校验和，防止数据篡改</li>
 * </ul>
 *
 */
public class ValidatedLengthFrameDecoder extends AbstractDecoder
{
    /** 报文头总长度：魔法值(4) + 长度(4) + CRC16(2) */
    public static final int HEADER_SIZE = 10;

    private final int magic;
    private final int minLength;
    private final int maxLength;

    /**
     * 创建解码器，最小长度默认为0
     *
     * @param magic     4字节魔法值
     * @param maxLength 最大报文体长度
     */
    public ValidatedLengthFrameDecoder(int magic, int maxLength)
    {
        this(magic, 0, maxLength);
    }

    /**
     * 创建解码器
     *
     * @param magic     4字节魔法值
     * @param minLength 最小报文体长度
     * @param maxLength 最大报文体长度
     */
    public ValidatedLengthFrameDecoder(int magic, int minLength, int maxLength)
    {
        if (minLength < 0)
        {
            throw new IllegalArgumentException("minLength must be >= 0");
        }
        if (maxLength < minLength)
        {
            throw new IllegalArgumentException("maxLength must be >= minLength");
        }
        this.magic     = magic;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        do
        {
            int remainRead = accumulation.remainRead();
            if (remainRead == 0)
            {
                accumulation.free();
                accumulation = null;
                return;
            }
            if (remainRead < HEADER_SIZE)
            {
                accumulation.compact();
                return;
            }
            int readPosi = accumulation.getReadPosi();
            int actualMagic = accumulation.getInt(readPosi);
            if (actualMagic != magic)
            {
                next.pipeline().shutdownInput();
                return;
            }
            int length = accumulation.getInt(readPosi + 4);
            if (length < minLength || length > maxLength)
            {
                throw new TooLongException();
            }
            short expectedCrc = CRC16Util.crc16(accumulation, 0, 8);
            short actualCrc   = accumulation.getShort(readPosi + 8);
            if (expectedCrc != actualCrc)
            {
                next.pipeline().shutdownInput();
                return;
            }
            int totalLength = HEADER_SIZE + length;
            if (remainRead < totalLength)
            {
                accumulation.compact();
                return;
            }
            accumulation.addReadPosi(HEADER_SIZE);
            IoBuffer packet = accumulation.slice(length);
            next.fireRead(packet);
        } while (true);
    }

    public int getMagic()
    {
        return magic;
    }

    public int getMinLength()
    {
        return minLength;
    }

    public int getMaxLength()
    {
        return maxLength;
    }
}
