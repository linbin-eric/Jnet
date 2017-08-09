package com.jfireframework.jnet.common.util;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.HeapByteBuf;

public class ByteBufFactory
{
    
    public static ByteBuf<?> allocate(int size)
    {
        return HeapByteBuf.allocate(size);
    }
    
    public static void release(ByteBuf<?> buf)
    {
    }
    
}
