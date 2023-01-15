package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

public class CachedPooledHeapBuffer extends PooledHeapBuffer implements CachedPooledBuffer<byte[]>
{
    protected ThreadCache<byte[]>                    cache;
    protected ThreadCache.MemoryCached<byte[]>       memoryCached;
    protected RecycleHandler<CachedPooledHeapBuffer> recycleHandler;
    static    Recycler<CachedPooledHeapBuffer>       RECYCLER = new Recycler<>(function -> {
        CachedPooledHeapBuffer                 buffer         = new CachedPooledHeapBuffer();
        RecycleHandler<CachedPooledHeapBuffer> recycleHandler = function.apply(buffer);
        buffer.recycleHandler = recycleHandler;
        return buffer;
    });

    public static CachedPooledHeapBuffer newOne()
    {
        return RECYCLER.get();
    }

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
                arena.free(chunkListNode, handle, capacity);
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
                arena.free(chunkListNode, handle, capacity);
            }
        }
        cache = null;
        memoryCached = null;
        recycleHandler.recycle(this);
    }
}
