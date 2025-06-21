package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.UnPooledStorageSegment;
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
        StorageSegment storageSegment = storageSegmentInstance();
        UnPooledBuffer buffer         = bufferInstance();
        if (preferDirect)
        {
            storageSegment.init(new byte[initializeCapacity], 0, 0, initializeCapacity);
        }
        else
        {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
            storageSegment.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, initializeCapacity);
        }
        buffer.init(storageSegment);
        return buffer;
    }

    @Override
    public String name()
    {
        return null;
    }

    @Override
    public UnPooledBuffer bufferInstance()
    {
        if (preferDirect)
        {
            return new UnPooledBuffer(BufferType.UNSAFE, this);
        }
        else
        {
            return new UnPooledBuffer(BufferType.HEAP, this);
        }
    }

    @Override
    public StorageSegment storageSegmentInstance()
    {
        return new UnPooledStorageSegment(this);
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        ;
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
        ;
    }
}
