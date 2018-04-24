package com.jfireframework.jnet.common.buffer;

public interface IoBufferAllocator
{
    AbstractIoBuffer allocate(int initSize);
    
    AbstractIoBuffer allocateDirect(int initSize);
    
    void release(AbstractIoBuffer ioBuffer);
}
