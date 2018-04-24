package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.buffer.AbstractIoBuffer;
import com.jfireframework.jnet.common.buffer.IoBufferAllocator;
import com.jfireframework.jnet.common.buffer.ThreadPooledIoBufferAllocator;

public class Allocator
{
    private static IoBufferAllocator allocator = new ThreadPooledIoBufferAllocator();
    
    public static AbstractIoBuffer allocate(int initSize)
    {
        return allocator.allocate(initSize);
    }
    
    public static AbstractIoBuffer allocateDirect(int initSize)
    {
        return allocator.allocateDirect(initSize);
    }
    
    public static void release(AbstractIoBuffer iobufer)
    {
        allocator.release(iobufer);
    }
}
