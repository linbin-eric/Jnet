package com.jfireframework.jnet.common.buffer;

import java.util.concurrent.atomic.AtomicInteger;
import com.jfireframework.jnet.common.recycler.Recycler;

public class ThreadPooledIoBufferAllocator implements IoBufferAllocator
{
    private Recycler<PooledIoBuffer> localHeap   = new Recycler<PooledIoBuffer>() {
                                               
                                               @Override
                                               protected PooledIoBuffer newObject(RecycleHandler handler)
                                               {
                                                   PooledIoBuffer heapIoBuffer = PooledIoBuffer.heapBuffer();
                                                   heapIoBuffer.recycleHandler = handler;
                                                   return heapIoBuffer;
                                               }
                                               
                                           };
    
    private Recycler<PooledIoBuffer> localDirect = new Recycler<PooledIoBuffer>() {
                                               
                                               @Override
                                               protected IoBuffer newObject(RecycleHandler handler)
                                               {
                                                   PooledIoBuffer directBuffer = PooledIoBuffer.directBuffer();
                                                   directBuffer.recycleHandler = handler;
                                                   return directBuffer;
                                               }
                                               
                                           };
    private Archon[]           heapArchons;
    private Archon[]           directArchons;
    
    private AtomicInteger      idx         = new AtomicInteger(0);
    private final int          mask;
    
    public ThreadPooledIoBufferAllocator()
    {
        mask = (1 << 3) - 1;
        heapArchons = new Archon[mask + 1];
        for (int i = 0; i < heapArchons.length; i++)
        {
            heapArchons[i] = Archon.heapPooledArchon(8, 128);
        }
        directArchons = new Archon[mask + 1];
        for (int i = 0; i < directArchons.length; i++)
        {
            directArchons[i] = Archon.directPooledArchon(8, 128);
        }
    }
    
    @Override
    public PooledIoBuffer allocate(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        PooledIoBuffer buffer = localHeap.get();
        if (buffer.archon == null)
        {
            heapArchons[index].apply(initSize, buffer);
        }
        return buffer;
    }
    
    @Override
    public PooledIoBuffer allocateDirect(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        PooledIoBuffer buffer = localDirect.get();
        if (buffer.archon == null)
        {
            directArchons[index].apply(initSize, buffer);
        }
        return buffer;
    }
    
    @Override
    public void release(PooledIoBuffer ioBuffer)
    {
        ioBuffer.clear();
        if (ioBuffer.recycleHandler.recycle(ioBuffer) == false)
        {
            ioBuffer.release();
        }
    }
    
}
