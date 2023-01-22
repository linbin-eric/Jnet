package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolHeapBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolUnsafeBuffer;
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
        UnPoolHeapBuffer buffer = new UnPoolHeapBuffer();
        buffer.init(new byte[initializeCapacity], initializeCapacity, 0);
        return buffer;
    }

    @Override
    public UnPoolUnsafeBuffer unsafeBuffer(int initializeCapacity)
    {
        UnPoolUnsafeBuffer buffer = new UnPoolUnsafeBuffer();
        buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity, 0);
        return buffer;
    }

    public UnPoolDirectBuffer directByteBuffer(int initializeCapacity)
    {
        UnPoolDirectBuffer buffer = new UnPoolDirectBuffer();
        buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity, 0);
        return buffer;
    }
//    public UnPoolMemoryBuffer memoryBuffer(int initializeCapacity)
//    {
//        UnPoolMemoryBuffer buffer  = new UnPoolMemoryBuffer();
//        MemorySession      session = MemorySession.openShared();
//        buffer.init(MemorySegment.allocateNative(initializeCapacity, session), initializeCapacity, 0);
//        return buffer;
//    }

    @Override
    public String name()
    {
        return null;
    }
}
