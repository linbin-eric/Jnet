package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.AbstractDirectByteBuffer;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolDirectByteBuffer extends AbstractDirectByteBuffer
{
    @Override
    protected long getAddress(ByteBuffer memory)
    {
        return PlatFormFunction.bytebufferOffsetAddress(memory);
    }

    @Override
    protected void reAllocate(int posi)
    {
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        ByteBuffer src = memory;
        memory = ByteBuffer.allocateDirect(posi);
        capacity = posi;
        memory.put(0, src, 0, writePosi);
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
