package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ProcessorContext;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class FixLengthDecoder extends AbstractDecoder
{
    private final int frameLength;

    /**
     * 固定长度解码器
     *
     * @param frameLength 一个报文的固定长度
     */
    public FixLengthDecoder(int frameLength, BufferAllocator allocator)
    {
        super(allocator);
        this.frameLength = frameLength;
    }

    @Override
    protected void process0(ProcessorContext ctx)
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
            if (remainRead < frameLength)
            {
                break;
            }
            IoBuffer packet = accumulation.slice(frameLength);
            ctx.fireRead(packet);
        } while (true);
        compactIfNeed();
    }
}
