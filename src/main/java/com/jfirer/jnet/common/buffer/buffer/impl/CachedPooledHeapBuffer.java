package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;

public class CachedPooledHeapBuffer extends PooledHeapBuffer implements CachedPooledBuffer<byte[]>
{
    protected ThreadCache<byte[]>              cache;
    protected ThreadCache.MemoryCached<byte[]> memoryCached;

    @Override
    public void init(ThreadCache.MemoryCached<byte[]> memoryCached, ThreadCache<byte[]> cache)
    {
        this.cache = cache;
        init(memoryCached.arena, memoryCached.chunkListNode, memoryCached.capacity, memoryCached.offset, memoryCached.handle);
    }

    @Override
    public void setCache(ThreadCache<byte[]> cache)
    {
        this.cache = cache;
    }

    @Override
    public void free0(int capacity)
    {
        if (memoryCached != null)
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
