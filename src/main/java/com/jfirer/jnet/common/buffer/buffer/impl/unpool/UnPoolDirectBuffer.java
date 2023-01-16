package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractDirectBuffer;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolDirectBuffer extends AbstractDirectBuffer
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
        long oldAddress   = address;
        long oldWritePosi = writePosi;
        memory = ByteBuffer.allocateDirect(posi);
        address = PlatFormFunction.bytebufferOffsetAddress(memory);
        capacity = posi;
        Bits.copyDirectMemory(oldAddress, address, oldWritePosi);
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
