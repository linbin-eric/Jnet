package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;

import java.nio.ByteBuffer;

public class CachedPooledDirectBuffer extends PooledDirectBuffer implements CachedPooledBuffer<ByteBuffer>
{
    protected ThreadCache<ByteBuffer>              cache;
    protected ThreadCache.MemoryCached<ByteBuffer> memoryCached;

    @Override
    public void init(ThreadCache.MemoryCached<ByteBuffer> memoryCached, ThreadCache<ByteBuffer> cache)
    {
        this.cache = cache;
        this.memoryCached = memoryCached;
        init(memoryCached.arena, memoryCached.chunkListNode, memoryCached.capacity, memoryCached.offset, memoryCached.handle);
    }

    @Override
    public void setCache(ThreadCache<ByteBuffer> cache)
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
