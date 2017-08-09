package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;

public class LengthEncodeProcessor implements StreamProcessor
{
    // 代表长度字段开始读取的位置
    private final int lengthFieldOffset;
    // 代表长度字段自身的长度。支持1,2,4.如果是1则使用unsignedbyte方式读取。如果是2则使用unsignedshort方式读取,4使用int方式读取。
    private final int lengthFieldLength;
    
    public LengthEncodeProcessor(int lengthFieldOffset, int lengthFieldLength)
    {
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFieldOffset = lengthFieldOffset;
    }
    
    @Override
    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
    {
        if (data instanceof ByteBuf)
        {
            ByteBuf<?> buf = (ByteBuf<?>) data;
            int length = buf.remainRead();
            switch (lengthFieldLength)
            {
                case 1:
                    buf.put(lengthFieldOffset, (byte) length);
                    break;
                case 2:
                    buf.writeShort(lengthFieldOffset, (short) length);
                    break;
                case 4:
                    buf.writeInt(lengthFieldOffset, length);
                    break;
                default:
                    break;
            }
            return buf;
        }
        else
        {
            return null;
        }
    }
    
}
