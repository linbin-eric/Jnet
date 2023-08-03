package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public class SliceSupportRelocateBufferAllocator implements BufferAllocator
{
    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        return null;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
    {
        return null;
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        return null;
    }

    @Override
    public IoBuffer unsafeBuffer(int initializeCapacity)
    {
        return null;
    }

    @Override
    public String name()
    {
        return null;
    }
}
