package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.AbstractMemorySegmentBuffer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

public class UnPoolMemoryBuffer extends AbstractMemorySegmentBuffer
{
    @Override
    protected long getAddress(MemorySegment memory)
    {
        return memory.address().toRawLongValue();
    }

    @Override
    protected void reAllocate(int posi)
    {
        MemorySession session = memory.session();
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        MemorySegment old        = memory;
        MemorySegment newSegment = MemorySegment.allocateNative(posi, session);
        capacity = posi;
        MemorySegment.copy(old, 0, newSegment, 0, writePosi);
        memory = newSegment;
    }

    @Override
    protected void free0(int capacity)
    {
        memory.session().close();
    }
}
