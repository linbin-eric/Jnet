package com.jfireframework.jnet.common.buffer;

public class UnPooledIoBufferAllocator implements IoBufferAllocator
{
    private Archon heapArchon   = UnPooledArchon.heapUnPooledArchon();
    private Archon directArchon = UnPooledArchon.directUnPooledArchon();
    
    @Override
    public IoBuffer allocate(int initSize)
    {
        IoBuffer buffer = IoBuffer.heapIoBuffer();
        heapArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public IoBuffer allocateDirect(int initSize)
    {
        IoBuffer buffer = IoBuffer.directBuffer();
        directArchon.apply(initSize, buffer);
        return buffer;
    }
    
    @Override
    public void release(IoBuffer ioBuffer)
    {
        ;
    }
    
}
