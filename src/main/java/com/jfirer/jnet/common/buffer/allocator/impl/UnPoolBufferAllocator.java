package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.util.SystemPropertyUtil;
import com.jfirer.jnet.common.util.UNSAFE;

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
        UnPooledBuffer buffer = new UnPooledBuffer(BufferType.HEAP);
        buffer.init(new byte[initializeCapacity], initializeCapacity, 0, 0);
        return buffer;
    }

    @Override
    public UnPooledBuffer unsafeBuffer(int initializeCapacity)
    {
        UnPooledBuffer buffer     = new UnPooledBuffer(BufferType.UNSAFE);
        ByteBuffer     byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
        buffer.init(byteBuffer, initializeCapacity, 0, UNSAFE.bytebufferOffsetAddress(byteBuffer));
        return buffer;
    }
//    public UnPoolDirectBuffer directByteBuffer(int initializeCapacity)
//    {
//        UnPoolDirectBuffer buffer     = new UnPoolDirectBuffer();
//        ByteBuffer         byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
//        buffer.init(byteBuffer, initializeCapacity, 0, PlatFormFunction.bytebufferOffsetAddress(byteBuffer));
//        return buffer;
//    }
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
