package com.jfireframework.jnet.common.buffer;

import java.util.concurrent.atomic.AtomicInteger;

public class PooledIoBufferAllocator implements IoBufferAllocator
{
    
    private Archon[]      heapArchons;
    private Archon[]      directArchons;
    
    private AtomicInteger idx = new AtomicInteger(0);
    private final int     mask;
    
    public PooledIoBufferAllocator()
    {
        mask = (1 << 3) - 1;
        heapArchons = new Archon[mask + 1];
        for (int i = 0; i < heapArchons.length; i++)
        {
            heapArchons[i] = PooledArchon.heapPooledArchon(8, 128);
        }
        directArchons = new Archon[mask + 1];
        for (int i = 0; i < directArchons.length; i++)
        {
            directArchons[i] = PooledArchon.directPooledArchon(8, 128);
        }
    }
    
    @Override
    public PooledIoBuffer allocate(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        PooledIoBuffer buffer = PooledIoBuffer.heapIoBuffer();
        heapArchons[index].apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public PooledIoBuffer allocateDirect(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        PooledIoBuffer buffer = PooledIoBuffer.directBuffer();
        directArchons[index].apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public void release(PooledIoBuffer ioBuffer)
    {
        ioBuffer.release();
    }
    
}
