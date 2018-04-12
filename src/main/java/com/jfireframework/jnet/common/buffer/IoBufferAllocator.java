package com.jfireframework.jnet.common.buffer;

public interface IoBufferAllocator
{
    IoBuffer allocate(int initSize);
    
    IoBuffer allocateDirect(int initSize);
    
    void release(IoBuffer ioBuffer);
}
