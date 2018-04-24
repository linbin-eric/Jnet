package com.jfireframework.jnet.common.buffer;

public class UnPooledIoBufferAllocator implements IoBufferAllocator
{
    private Archon heapArchon   = UnPooledArchon.heapUnPooledArchon();
    private Archon directArchon = UnPooledArchon.directUnPooledArchon();
    
    @Override
    public AbstractIoBuffer allocate(int initSize)
    {
        AbstractIoBuffer buffer = AbstractIoBuffer.heapIoBuffer();
        heapArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public AbstractIoBuffer allocateDirect(int initSize)
    {
        AbstractIoBuffer buffer = AbstractIoBuffer.directBuffer();
        directArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public void release(AbstractIoBuffer ioBuffer)
    {
        ;
    }
    
}
