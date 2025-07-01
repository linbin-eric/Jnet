package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer2;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolBufferAllocator2 implements BufferAllocator
{
    private final       boolean                preferDirect;
    public static final UnPoolBufferAllocator2 DEFAULT = new UnPoolBufferAllocator2(false);

    public UnPoolBufferAllocator2()
    {
        this(true);
    }

    public UnPoolBufferAllocator2(boolean preferDirect)
    {
        this.preferDirect = preferDirect;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        UnPooledBuffer2 buffer = bufferInstance();
        if (preferDirect)
        {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
            buffer.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), initializeCapacity, 0, 0, initializeCapacity);
        }
        else
        {
            buffer.init(new byte[initializeCapacity], 0, initializeCapacity, 0, 0, initializeCapacity);
        }
        return buffer;
    }

    @Override
    public String name()
    {
        return null;
    }

    @Override
    public UnPooledBuffer2 bufferInstance()
    {
        if (preferDirect)
        {
            return new UnPooledBuffer2(BufferType.UNSAFE, this);
        }
        else
        {
            return new UnPooledBuffer2(BufferType.HEAP, this);
        }
    }

    @Override
    public StorageSegment storageSegmentInstance()
    {
        throw new UnsupportedOperationException();
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
