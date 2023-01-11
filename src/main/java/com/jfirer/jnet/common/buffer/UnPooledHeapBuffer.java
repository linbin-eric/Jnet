package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractHeapBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.SliceHeapBuffer;

public class UnPooledHeapBuffer extends AbstractHeapBuffer implements UnPooledBuffer<byte[]>
{
    @Override
    protected long getAddress(byte[] memory)
    {
        return 0;
    }

    @Override
    protected void reAllocate(int newCapacity)
    {
        byte[] oldMemory = memory;
        memory = new byte[newCapacity];
        System.arraycopy(oldMemory, 0, memory, 0, writePosi);
        capacity = newCapacity;
    }

    @Override
    protected void free0(int capacity)
    {
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceHeapBuffer.slice(this, length);
    }
}
