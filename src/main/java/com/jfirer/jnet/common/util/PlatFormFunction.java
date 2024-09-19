package com.jfirer.jnet.common.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class PlatFormFunction
{
    static final long address;

    static
    {
//        address = UNSAFE.getFieldOffset("address", Buffer.class);
        address = UNSAFE.unsafe.objectFieldOffset(Buffer.class, "address");
    }

    public static long bytebufferOffsetAddress(ByteBuffer buffer)
    {
//        System.out.println("调用");
        return UNSAFE.unsafe.getLong(buffer, address);
    }
}
