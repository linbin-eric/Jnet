package com.jfireframework.jnet.common.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class PlatFormFunction
{
    private static final Unsafe UNSAFE;
    private static final long   BYTEBUFFER_OFFSET_ADDRESS = UnsafeFieldAccess.getFieldOffset("address", Buffer.class);
    static
    {
        Object unsafe;
        try
        {
            unsafe = ReflectUtil.getUnsafe();
        }
        catch (Exception e)
        {
            unsafe = null;
        }
        UNSAFE = (Unsafe) unsafe;
    }
    
    static boolean hasUnsafe()
    {
        return UNSAFE != null;
    }
    
    public static long bytebufferOffsetAddress(ByteBuffer buffer)
    {
        return UNSAFE.getLong(buffer, BYTEBUFFER_OFFSET_ADDRESS);
    }
}
