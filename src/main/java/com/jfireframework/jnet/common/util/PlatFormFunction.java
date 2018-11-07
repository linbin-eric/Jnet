package com.jfireframework.jnet.common.util;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

@SuppressWarnings("restriction")
public class PlatFormFunction
{
    public static long bytebufferOffsetAddress(ByteBuffer buffer)
    {
        return ((DirectBuffer) buffer).address();
    }

    public static sun.misc.Cleaner bytebufferCleaner(ByteBuffer buffer)
    {
        return ((DirectBuffer) buffer).cleaner();
    }
}
