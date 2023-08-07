package com.jfirer.jnet.common.decoder;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

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
            if (remainRead < frameLength)
            {
                break;
            }
            IoBuffer packet = accumulation.slice(frameLength);
            next.fireRead(packet);
        } while (true);
        accumulation.compact();
    }
}
