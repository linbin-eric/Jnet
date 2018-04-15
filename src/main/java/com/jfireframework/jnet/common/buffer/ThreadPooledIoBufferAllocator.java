package com.jfireframework.jnet.common.buffer;

import java.util.concurrent.atomic.AtomicInteger;
import com.jfireframework.jnet.common.recycler.Recycler;

public class ThreadPooledIoBufferAllocator implements IoBufferAllocator
{
    private Recycler<IoBuffer> localHeap   = new Recycler<IoBuffer>() {
                                               
                                               @Override
                                               protected IoBuffer newObject(RecycleHandler handler)
                                               {
                                                   IoBuffer heapIoBuffer = IoBuffer.heapIoBuffer();
                                                   heapIoBuffer.recycleHandler = handler;
                                                   return heapIoBuffer;
                                               }
                                               
                                           };
    
    private Recycler<IoBuffer> localDirect = new Recycler<IoBuffer>() {
                                               
                                               @Override
                                               protected IoBuffer newObject(RecycleHandler handler)
                                               {
                                                   IoBuffer directBuffer = IoBuffer.directBuffer();
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
            heapArchons[i] = PooledArchon.heapPooledArchon(8, 128);
        }
        directArchons = new Archon[mask + 1];
        for (int i = 0; i < directArchons.length; i++)
        {
            directArchons[i] = PooledArchon.directPooledArchon(8, 128);
        }
    }
    
    @Override
    public IoBuffer allocate(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        IoBuffer buffer = localHeap.get();
        if (buffer.archon == null)
        {
            heapArchons[index].apply(initSize, buffer);
        }
        return buffer;
    }
    
    @Override
    public IoBuffer allocateDirect(int initSize)
    {
        int index = idx.incrementAndGet() & mask;
        IoBuffer buffer = localDirect.get();
        if (buffer.archon == null)
        {
            directArchons[index].apply(initSize, buffer);
        }
        return buffer;
    }
    
    @Override
    public void release(IoBuffer ioBuffer)
    {
        ioBuffer.clearData();
        if (ioBuffer.recycleHandler.recycle(ioBuffer) == false)
        {
            ioBuffer.release();
        }
    }
    
}
