package com.jfirer.jnet.common.buffer.buffer.impl.unpool;

import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableHeapBuffer;

public class UnPoolHeapBuffer extends CacheablePoolableHeapBuffer
{
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
