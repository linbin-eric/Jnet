package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class UnPooledRecycledBufferAllocator implements BufferAllocator
{
    public static UnPooledRecycledBufferAllocator DEFAULT             = new UnPooledRecycledBufferAllocator("UnPooledRecycledBufferAllocator_default");
    private       Recycler<UnPooledHeapBuffer>    unPooledHeapBuffers = new Recycler<UnPooledHeapBuffer>()
    {
        @Override
        protected UnPooledHeapBuffer newObject(RecycleHandler handler)
        {
            UnPooledHeapBuffer buffer = new UnPooledHeapBuffer();
            buffer.recycleHandler = handler;
            return buffer;
        }

        ;
    };
    private       Recycler<UnPooledDirectBuffer>  unPooledDirectBuffers = new Recycler<UnPooledDirectBuffer>()
    {
        @Override
        protected UnPooledDirectBuffer newObject(RecycleHandler handler)
        {
            UnPooledDirectBuffer buffer = new UnPooledDirectBuffer();
            buffer.recycleHandler = handler;
            return buffer;
        }

        ;
    };
    private       boolean                         preferDirect          = true;
    private       String                          name;

    public UnPooledRecycledBufferAllocator(String name)
    {
        this(true, name);
    }

    public UnPooledRecycledBufferAllocator(boolean preferDirect, String name)
    {
        this.preferDirect = preferDirect;
        this.name = name;
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        AbstractBuffer buffer = unPooledHeapBuffers.get();
        buffer.init(new byte[initializeCapacity], initializeCapacity,0,0,0);
        return buffer;
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        AbstractBuffer buffer = unPooledDirectBuffers.get();
        buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity,0,0,0);
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
