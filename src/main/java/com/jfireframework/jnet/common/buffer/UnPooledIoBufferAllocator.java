package com.jfireframework.jnet.common.buffer;

public class UnPooledIoBufferAllocator implements IoBufferAllocator
{
    private Archon heapArchon   = UnPooledArchon.heapUnPooledArchon();
    private Archon directArchon = UnPooledArchon.directUnPooledArchon();
    
    @Override
    public PooledIoBuffer allocate(int initSize)
    {
        PooledIoBuffer buffer = PooledIoBuffer.heapIoBuffer();
        heapArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public PooledIoBuffer allocateDirect(int initSize)
    {
        PooledIoBuffer buffer = PooledIoBuffer.directBuffer();
        directArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public void release(PooledIoBuffer ioBuffer)
    {
        ;
    }
    
}
