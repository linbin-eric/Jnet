package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.nio.ByteBuffer;

public class UnPoolBufferAllocator implements BufferAllocator
{
    public static final boolean PREFER_DIRECT = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        return ioBuffer(initializeCapacity, PREFER_DIRECT);
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
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

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        BasicBuffer    buffer         = new BasicBuffer(BufferType.HEAP);
        StorageSegment storageSegment = StorageSegment.POOL.get();
        storageSegment.init(new byte[initializeCapacity], 0, 0, initializeCapacity);
        buffer.init(storageSegment);
        return buffer;
    }

    @Override
    public BasicBuffer unsafeBuffer(int initializeCapacity)
    {
        BasicBuffer buffer     = new BasicBuffer(BufferType.UNSAFE);
        ByteBuffer  byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
        StorageSegment storageSegment =
                StorageSegment.POOL.get();
        storageSegment.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, initializeCapacity);
        buffer.init(storageSegment);
        return buffer;
    }

    @Override
    public String name()
    {
        return null;
    }
}
