package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.BufferType;

public class CacheablePooledBuffer extends PooledBuffer
{
    protected ThreadCache              cache;
    protected ThreadCache.MemoryCached memoryCached;

    public CacheablePooledBuffer(BufferType bufferType)
    {
        super(bufferType);
    }

    public void init(ThreadCache.MemoryCached memoryCached, ThreadCache cache)
    {
        this.cache = cache;
        this.memoryCached = memoryCached;
        init(memoryCached.arena, memoryCached.chunkListNode, memoryCached.capacity, memoryCached.offset, memoryCached.handle);
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        if (cache == null)
        {
            super.reAllocate(reqCapacity);
        }
        else
        {
            ThreadCache.MemoryCached oldMemoryCached = this.memoryCached;
            ThreadCache              oldCache        = this.cache;
            cache = null;
            memoryCached = null;
            oldCache.reAllocate(oldMemoryCached, reqCapacity, this);
        }
    }

    @Override
    protected void free0(int capacity)
    {
        if (cache == null)
        {
            super.free0(capacity);
        }
        else
        {
            cache.free(memoryCached);
            cache = null;
            memoryCached = null;
        }
    }
}
