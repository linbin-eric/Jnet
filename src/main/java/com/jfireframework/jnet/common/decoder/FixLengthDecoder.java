package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

public class FixLengthDecoder extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    private final int             frameLength;
    private       BufferAllocator allocator;

    /**
     * 固定长度解码器
     *
     * @param frameLength 一个报文的固定长度
     */
    public FixLengthDecoder(int frameLength, BufferAllocator allocator)
    {
        this.frameLength = frameLength;
        this.allocator = allocator;
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
    }

    @Override
    public void process(IoBuffer ioBuf) throws Throwable
    {
        do
        {
            if (ioBuf.remainRead() < frameLength)
            {
                ioBuf.compact().capacityReadyFor(frameLength);
                break;
            }
            IoBuffer packet = allocator.ioBuffer(frameLength);
            packet.put(ioBuf, frameLength);
            ioBuf.addReadPosi(frameLength);
            downStream.process(packet);
            break;
        } while (true);
    }
}
