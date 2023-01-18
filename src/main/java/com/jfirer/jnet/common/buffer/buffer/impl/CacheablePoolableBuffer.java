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

    public void setCache(ThreadCache<T> cache)
    {
        this.cache = cache;
    }

    @Override
    protected void free0(int capacity)
    {
        if (cache == null)
        {
            super.free0(capacity);
            return;
        }
        else if (memoryCached != null)
        {
            if (cache.add(memoryCached))
            {
                ;
            }
            else
            {
                super.free0(capacity);
            }
        }
        else
        {
            if (cache.add(arena, chunkListNode, capacity, offset, handle))
            {
                ;
            }
            else
            {
                super.free0(capacity);
            }
        }
        cache = null;
        memoryCached = null;
    }
}
