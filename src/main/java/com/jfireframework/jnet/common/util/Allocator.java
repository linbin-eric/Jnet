package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.IoBufferAllocator;
import com.jfireframework.jnet.common.buffer.ThreadPooledIoBufferAllocator;

public class Allocator
{
    private static IoBufferAllocator allocator = new ThreadPooledIoBufferAllocator();
    
    public static IoBuffer allocate(int initSize)
    {
        return allocator.allocate(initSize);
    }
    
    public static IoBuffer allocateDirect(int initSize)
    {
        return allocator.allocateDirect(initSize);
    }
    
    public static void release(IoBuffer iobufer)
    {
        allocator.release(iobufer);
    }
}
