package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

public class LengthEncoder extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    // 代表长度字段开始读取的位置
    private final int            lengthFieldOffset;
    // 代表长度字段自身的长度。支持1,2,4.如果是1则使用unsignedbyte方式读取。如果是2则使用unsignedshort方式读取,4使用int方式读取。
    private final int            lengthFieldLength;
    private       ChannelContext channelContext;

    public LengthEncoder(int lengthFieldOffset, int lengthFieldLength)
    {
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFieldOffset = lengthFieldOffset;
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public void process(IoBuffer buf) throws Throwable
    {
        int length = buf.remainRead();
        switch (lengthFieldLength)
        {
            case 1:
                buf.put((byte) length, lengthFieldOffset);
                break;
            case 2:
                buf.putShort((short) length, lengthFieldOffset);
                break;
            case 4:
                buf.putInt(length, lengthFieldOffset);
                break;
            default:
                break;
        }
        downStream.process(buf);
    }
}
