package com.jfireframework.jnet.common.buffer;

public interface IoBufferAllocator
{
    PooledIoBuffer allocate(int initSize);
    
    PooledIoBuffer allocateDirect(int initSize);
    
    void release(PooledIoBuffer ioBuffer);
}
