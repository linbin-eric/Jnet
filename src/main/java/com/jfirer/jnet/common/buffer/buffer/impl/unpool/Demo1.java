package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

public class Demo1
{
    public static void main(String[] args)
    {
        MemorySession session = MemorySession.openShared();
        MemorySegment segment = MemorySegment.allocateNative(100, session);
        segment.set(ValueLayout.JAVA_BYTE, 1L, (byte) 2);
        byte b = segment.get(ValueLayout.JAVA_BYTE, 1L);
        System.out.println(b);
    }
}
