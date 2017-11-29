package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;

public class LengthEncoder implements ReadProcessor
{
    // 代表长度字段开始读取的位置
    private final int lengthFieldOffset;
    // 代表长度字段自身的长度。支持1,2,4.如果是1则使用unsignedbyte方式读取。如果是2则使用unsignedshort方式读取,4使用int方式读取。
    private final int lengthFieldLength;
    
    public LengthEncoder(int lengthFieldOffset, int lengthFieldLength)
    {
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFieldOffset = lengthFieldOffset;
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
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
            channelContext.write(buf);
        }
        else
        {
            chain.chain(data);
        }
    }
    
}
