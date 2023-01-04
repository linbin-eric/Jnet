package com.jfirer.jnet.common.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class PlatFormFunction
{
    static final long address;
    static
    {
        address = UNSAFE.getFieldOffset("address", Buffer.class);
    }
    public static long bytebufferOffsetAddress(ByteBuffer buffer)
    {
        return UNSAFE.getLong(buffer, address);
    }
}
