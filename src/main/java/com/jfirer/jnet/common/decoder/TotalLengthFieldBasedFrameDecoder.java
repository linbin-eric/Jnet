package com.jfirer.jnet.common.decoder;

import com.jfirer.jnet.common.api.ProcessorContext;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;
import com.jfirer.jnet.common.exception.TooLongException;

/**
 * 报文长度整体frame解码器。其中需要解码的长度所代表的长度信息是保温整体的长度信息，也就是包含报文头和报文体一起的总长度
 *
 * @author eric(eric @ jfire.cn)
 */
public class TotalLengthFieldBasedFrameDecoder extends AbstractDecoder
{
    // 代表长度字段开始读取的位置
    private final int lengthFieldOffset;
    // 代表长度字段自身的长度。支持1,2,4.如果是1则使用unsignedbyte方式读取。如果是2则使用unsignedshort方式读取,4使用int方式读取。
    private final int lengthFieldLength;
    // 将长度字段读取完毕，需要的偏移量,就是上面两个值相加
    private final int lengthFieldEndOffset;
    // 需要忽略的字节数
    private final int skipBytes;
    private final int maxLegnth;

    /**
     * @param lengthFieldOffset 长度字段在报文中的偏移量
     * @param lengthFieldLength 长度字段本身的长度
     * @param skipBytes         解析后的报文需要跳过的位数
     * @param maxLength
     */
    public TotalLengthFieldBasedFrameDecoder(int lengthFieldOffset, int lengthFieldLength, int skipBytes, int maxLength, BufferAllocator allocator)
    {
        super(allocator);
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.maxLegnth = maxLength;
        this.skipBytes = skipBytes;
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
    }


    @Override
    public void process0(ProcessorContext ctx)
    {
        do
        {
            int maskReadPosi = accumulation.getReadPosi();
            int left         = accumulation.remainRead();
            if (left == 0)
            {
                accumulation.free();
                accumulation = null;
                break;
            }
            if (lengthFieldEndOffset > left)
            {
                compactIfNeed();
                break;
            }
            // iobuffer中可能包含好几个报文，所以这里应该是增加的方式而不是直接设置的方式
            accumulation.addReadPosi(lengthFieldOffset);
            // 获取到整体报文的长度
            int length = 0;
            switch (lengthFieldLength)
            {
                case 1:
                    length = accumulation.get() & 0xff;
                    break;
                case 2:
                    length = accumulation.getShort() & 0xff;
                    break;
                case 4:
                    length = accumulation.getInt();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            // 得到整体长度后，开始从头读取这个长度的内容
            accumulation.setReadPosi(maskReadPosi);
            if (length >= maxLegnth)
            {
                throw new TooLongException();
            }
            if (length > accumulation.remainRead())
            {
                compactIfNeed();
                break;
            }
            else
            {
                IoBuffer packet = accumulation.slice(length);
                if (skipBytes != 0)
                {
                    packet.addReadPosi(skipBytes);
                }
                ctx.fireRead(packet);
            }
        } while (true);
    }
}
