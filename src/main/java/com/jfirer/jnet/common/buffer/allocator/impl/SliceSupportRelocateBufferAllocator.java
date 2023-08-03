package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.SliceSupportReAllocateBuffer;
import com.jfirer.jnet.common.recycler.Recycler;

public class SliceSupportRelocateBufferAllocator implements BufferAllocator
{
    public static SliceSupportRelocateBufferAllocator DEFAULT = new SliceSupportRelocateBufferAllocator(PooledBufferAllocator.DEFAULT);

    private PooledBufferAllocator                  allocator;
    private Recycler<SliceSupportReAllocateBuffer> recycler = new Recycler<>(SliceSupportReAllocateBuffer::new, SliceSupportReAllocateBuffer::setHandler);

    public SliceSupportRelocateBufferAllocator(PooledBufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        PooledBuffer                 delegation = (PooledBuffer) allocator.ioBuffer(initializeCapacity);
        SliceSupportReAllocateBuffer buffer     = recycler.get();
        buffer.resetDelegation(delegation, allocator);
        return buffer;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
    {
        PooledBuffer                 delegation = (PooledBuffer) allocator.ioBuffer(initializeCapacity, direct);
        SliceSupportReAllocateBuffer buffer     = recycler.get();
        buffer.resetDelegation(delegation, allocator);
        return buffer;
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        PooledBuffer                 delegation = (PooledBuffer) allocator.ioBuffer(initializeCapacity, false);
        SliceSupportReAllocateBuffer buffer     = recycler.get();
        buffer.resetDelegation(delegation, allocator);
        return buffer;
    }

    @Override
    public IoBuffer unsafeBuffer(int initializeCapacity)
    {
        PooledBuffer                 delegation = (PooledBuffer) allocator.ioBuffer(initializeCapacity, true);
        SliceSupportReAllocateBuffer buffer     = recycler.get();
        buffer.resetDelegation(delegation, allocator);
        return buffer;
    }

    @Override
    public String name()
    {
        return SliceSupportRelocateBufferAllocator.class.getName();
    }
}
