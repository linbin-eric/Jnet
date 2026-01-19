package cc.jfire.jnet.common.coder;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

/**
 * CRC16 校验和工具类
 * 使用 CRC16-CCITT 算法（多项式 0x1021）
 *
 * @author eric(eric @ jfire.cn)
 */
public class CRC16Util
{
    private static final int POLYNOMIAL = 0x1021;
    private static final int INITIAL    = 0xFFFF;

    /**
     * 计算字节数组的 CRC16 校验和
     *
     * @param data   字节数组
     * @param offset 起始偏移
     * @param length 长度
     * @return CRC16 校验和（16位）
     */
    public static short crc16(byte[] data, int offset, int length)
    {
        int crc = INITIAL;
        for (int i = offset; i < offset + length; i++)
        {
            crc ^= (data[i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++)
            {
                if ((crc & 0x8000) != 0)
                {
                    crc = (crc << 1) ^ POLYNOMIAL;
                }
                else
                {
                    crc <<= 1;
                }
            }
        }
        return (short) (crc & 0xFFFF);
    }

    /**
     * 计算 IoBuffer 指定范围的 CRC16 校验和（不改变读位置）
     *
     * @param buffer IoBuffer 缓冲区
     * @param offset 相对于当前读位置的偏移
     * @param length 要计算的字节长度
     * @return CRC16 校验和（16位）
     */
    public static short crc16(IoBuffer buffer, int offset, int length)
    {
        int crc      = INITIAL;
        int startPos = buffer.getReadPosi() + offset;
        for (int i = 0; i < length; i++)
        {
            byte b = buffer.get(startPos + i);
            crc ^= (b & 0xFF) << 8;
            for (int j = 0; j < 8; j++)
            {
                if ((crc & 0x8000) != 0)
                {
                    crc = (crc << 1) ^ POLYNOMIAL;
                }
                else
                {
                    crc <<= 1;
                }
            }
        }
        return (short) (crc & 0xFFFF);
    }
}
