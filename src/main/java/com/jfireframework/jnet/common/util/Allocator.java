package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.buffer.IoBufferAllocator;
import com.jfireframework.jnet.common.buffer.ThreadPooledIoBufferAllocator;

public class Allocator
{
    private static IoBufferAllocator allocator = new ThreadPooledIoBufferAllocator();
    
    public static PooledIoBuffer allocate(int initSize)
    {
        return allocator.allocate(initSize);
    }
    
    public static PooledIoBuffer allocateDirect(int initSize)
    {
        return allocator.allocateDirect(initSize);
    }
    
    public static void release(PooledIoBuffer iobufer)
    {
        allocator.release(iobufer);
    }
}
