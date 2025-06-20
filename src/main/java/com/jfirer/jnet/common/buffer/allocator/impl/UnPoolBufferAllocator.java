package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolBufferAllocator implements BufferAllocator
{
    private final boolean preferDirect;

    public UnPoolBufferAllocator()
    {
        this(true);
    }

    public UnPoolBufferAllocator(boolean preferDirect)
    {
        this.preferDirect = preferDirect;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        return ioBuffer(initializeCapacity, preferDirect);
    }

    private IoBuffer ioBuffer(int initializeCapacity, boolean direct)
    {
        if (direct)
        {
            return unsafeBuffer(initializeCapacity);
        }
        else
        {
            return heapBuffer(initializeCapacity);
        }
    }

    private IoBuffer heapBuffer(int initializeCapacity)
    {
        BasicBuffer    buffer         = new BasicBuffer(BufferType.HEAP);
        StorageSegment storageSegment = new StorageSegment();
        storageSegment.init(new byte[initializeCapacity], 0, 0, initializeCapacity);
        buffer.init(storageSegment);
        return buffer;
    }

    private BasicBuffer unsafeBuffer(int initializeCapacity)
    {
        BasicBuffer    buffer         = new BasicBuffer(BufferType.UNSAFE);
        ByteBuffer     byteBuffer     = ByteBuffer.allocateDirect(initializeCapacity);
        StorageSegment storageSegment = new StorageSegment();
        storageSegment.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, initializeCapacity);
        buffer.init(storageSegment);
        return buffer;
    }

    @Override
    public String name()
    {
        return null;
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
        throw new UnsupportedOperationException();
    }
}
