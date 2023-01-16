package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.AbstractHeapBuffer;

public class UnPoolHeapBuffer extends AbstractHeapBuffer
{
    @Override
    protected long getAddress(byte[] memory)
    {
        return 0;
    }

    @Override
    protected void reAllocate(int posi)
    {
        byte[] oldMemory = memory;
        memory = new byte[posi];
        System.arraycopy(oldMemory, 0, memory, 0, writePosi);
        capacity = posi;
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
