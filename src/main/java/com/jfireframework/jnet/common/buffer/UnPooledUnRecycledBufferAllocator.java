package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class UnPooledUnRecycledBufferAllocator implements BufferAllocator
{
    public static final UnPooledUnRecycledBufferAllocator DEFAULT = new UnPooledUnRecycledBufferAllocator("UnPooledUnRecycledBufferAllocator_default");
    private             boolean                           preferDirect;
    private             String                            name;

    public UnPooledUnRecycledBufferAllocator(boolean preferDirect, String name)
    {
        this.preferDirect = preferDirect;
        this.name = name;
    }

    public UnPooledUnRecycledBufferAllocator(String name)
    {
        this(true, name);
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        UnPooledBuffer<byte[]> buffer = new UnPooledHeapBuffer();
        buffer.init(new byte[initializeCapacity], initializeCapacity);
        return buffer;
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        UnPooledBuffer<ByteBuffer> buffer = new UnPooledDirectBuffer();
        buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity);
        return buffer;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        if (preferDirect)
        {
            return directBuffer(initializeCapacity);
        }
        else
        {
            return heapBuffer(initializeCapacity);
        }
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
    {
        if (direct)
        {
            return directBuffer(initializeCapacity);
        }
        else
        {
            return heapBuffer(initializeCapacity);
        }
    }

    @Override
    public String name()
    {
        return name;
    }
}
