package com.jfirer.jnet.common.util;

import org.jctools.util.UnsafeAccess;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class PlatFormFunction
{
    static final long address;
    static
    {
//        address = UNSAFE.getFieldOffset("address", Buffer.class);
        address = UnsafeAccess.fieldOffset(Buffer.class, "address");
    }
    public static long bytebufferOffsetAddress(ByteBuffer buffer)
    {
//        System.out.println("调用");
        return UnsafeAccess.UNSAFE.getLong(buffer, address);
    }
}
