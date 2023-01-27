package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;

public abstract class CacheablePoolableBuffer<T> extends PoolableBuffer<T>
{
    protected ThreadCache<T>              cache;
    protected ThreadCache.MemoryCached<T> memoryCached;

    public void init(ThreadCache.MemoryCached<T> memoryCached, ThreadCache<T> cache)
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
            ThreadCache.MemoryCached<T> oldMemoryCached = this.memoryCached;
            ThreadCache<T>              oldCache        = this.cache;
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
