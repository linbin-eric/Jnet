package com.jfireframework.jnet.common.buffer;

public class UnPooledHeapBuffer extends AbstractHeapBuffer implements UnPooledBuffer
{

    @Override
    protected void reAllocate(int newCapacity)
    {
        byte[] oldMemory = memory;
        memory = new byte[newCapacity];
        System.arraycopy(oldMemory, 0, memory, 0, writePosi);
        capacity = newCapacity;
    }

    @Override
    protected void free0()
    {
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceHeapBuffer.slice(this, length);
    }
}
