package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableDirectBuffer;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolDirectBuffer extends CacheablePoolableDirectBuffer
{
    @Override
    protected long getNativeAddress(ByteBuffer memory)
    {
        return PlatFormFunction.bytebufferOffsetAddress(memory);
    }

    @Override
    protected void reAllocate(int posi)
    {
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        ByteBuffer src          = memory;
        int        oldReadPosi  = readPosi;
        int        oldWritePosi = writePosi;
        memory = ByteBuffer.allocateDirect(posi);
        init(memory, posi, 0);
        readPosi = oldReadPosi;
        writePosi = oldWritePosi;
        memory.put(0, src, 0, writePosi);
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
