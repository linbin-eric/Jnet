package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPooledDirectBuffer extends AbstractDirectBuffer implements UnPooledBuffer
{

    @Override
    protected void reAllocate(int newCapacity)
    {
        newCapacity = newCapacity > capacity * 2 ? newCapacity : 2 * capacity;
        ByteBuffer oldMemory = memory;
        memory = ByteBuffer.allocateDirect(newCapacity);
        oldMemory.position(0).limit(writePosi);
        memory.put(oldMemory);
        memory.position(0).limit(newCapacity);
        capacity = newCapacity;
        address = PlatFormFunction.bytebufferOffsetAddress(memory);
    }

    @Override
    protected void free0()
    {
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
