package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableUnsafeBuffer;

import java.nio.ByteBuffer;

public class UnPoolUnsafeBuffer extends CacheablePoolableUnsafeBuffer
{
    @Override
    protected void reAllocate(int posi)
    {
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        long oldAddress   = selfAddress;
        int  oldReadPosi  = readPosi;
        int  oldWritePosi = writePosi;
        memory = ByteBuffer.allocateDirect(posi);
        init(memory, posi, 0);
        readPosi = oldReadPosi;
        writePosi = oldWritePosi;
        Bits.copyDirectMemory(oldAddress, selfAddress, oldWritePosi);
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
