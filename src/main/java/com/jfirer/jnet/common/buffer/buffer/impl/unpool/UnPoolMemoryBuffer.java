package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableMemoryBuffer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

public class UnPoolMemoryBuffer extends CacheablePoolableMemoryBuffer
{
    @Override
    protected long getNativeAddress(MemorySegment memory)
    {
        return memory.address().toRawLongValue();
    }

    @Override
    protected void reAllocate(int posi)
    {
        MemorySession session = memory.session();
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        MemorySegment old          = memory;
        int           oldReadPosi  = readPosi;
        int           oldWritePosi = writePosi;
        memory = MemorySegment.allocateNative(posi, session);
        init(memory, posi, 0);
        readPosi = oldReadPosi;
        writePosi = oldWritePosi;
        MemorySegment.copy(old, 0, memory, 0, writePosi);
    }

    @Override
    protected void free0(int capacity)
    {
        memory.session().close();
    }
}
