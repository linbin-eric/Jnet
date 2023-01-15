package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class CachedPooledDirectBuffer extends PooledDirectBuffer implements CachedPooledBuffer<ByteBuffer>
{
    protected ThreadCache<ByteBuffer>                  cache;
    protected ThreadCache.MemoryCached<ByteBuffer>     memoryCached;
    protected RecycleHandler<CachedPooledDirectBuffer> recycleHandler;
    static    Recycler<CachedPooledDirectBuffer>       RECYCLER = new Recycler<>(function -> {
        CachedPooledDirectBuffer                 buffer         = new CachedPooledDirectBuffer();
        RecycleHandler<CachedPooledDirectBuffer> recycleHandler = function.apply(buffer);
        buffer.recycleHandler = recycleHandler;
        return buffer;
    });

    public static CachedPooledDirectBuffer newOne()
    {
        return RECYCLER.get();
    }

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
