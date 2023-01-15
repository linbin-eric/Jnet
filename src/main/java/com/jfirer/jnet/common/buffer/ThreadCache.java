package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.util.MathUtil;
import org.jctools.queues.MpscArrayQueue;

public class ThreadCache<T>
{
    public static class MemoryCached<T>
    {
        public Arena<T>         arena;
        public ChunkListNode<T> chunkListNode;
        public int              capacity;
        public int              offset;
        public long             handle;
        RecycleHandler<MemoryCached<T>> recycleHandler;
    }

    private MpscArrayQueue<MemoryCached<T>>[] regionCaches;
    Recycler<MemoryCached<T>> recycler = new Recycler<>(function -> {
        MemoryCached   memoryCached   = new MemoryCached();
        RecycleHandler recycleHandler = function.apply(memoryCached);
        memoryCached.recycleHandler = recycleHandler;
        return memoryCached;
    });

    public ThreadCache(int numOfCached, int maxCachedCapacity)
    {
        regionCaches = new MpscArrayQueue[MathUtil.log2(maxCachedCapacity) - 4];
        for (int i = 0; i < regionCaches.length; i++)
        {
            regionCaches[i] = new MpscArrayQueue<>(numOfCached);
        }
    }

    public boolean add(MemoryCached<T> memoryCached)
    {
        int index = MathUtil.log2(MathUtil.normalizeSize(memoryCached.capacity)) - 4;
        if (index < regionCaches.length)
        {
            MpscArrayQueue<MemoryCached<T>> regionCache = regionCaches[index];
            if (regionCache.offer(memoryCached))
            {
                return true;
            }
            else
            {
                memoryCached.recycleHandler.recycle(memoryCached);
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean allocate(int capacity, CachedPooledBuffer<T> buffer)
    {
        int index = MathUtil.log2(MathUtil.normalizeSize(capacity)) - 4;
        if (index < regionCaches.length)
        {
            MpscArrayQueue<MemoryCached<T>> regionCache = regionCaches[index];
            MemoryCached<T>                 cached      = regionCache.poll();
            if (cached == null)
            {
                return false;
            }
            else
            {
                buffer.init(cached, this);
                return true;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean add(Arena<T> arena, ChunkListNode<T> chunkListNode, int capacity, int offset, long handle)
    {
        int index = MathUtil.log2(MathUtil.normalizeSize(capacity)) - 4;
        if (index < regionCaches.length)
        {
            MpscArrayQueue<MemoryCached<T>> regionCache  = regionCaches[index];
            MemoryCached<T>                 memoryCached = recycler.get();
            memoryCached.arena = arena;
            memoryCached.chunkListNode = chunkListNode;
            memoryCached.capacity = capacity;
            memoryCached.offset = offset;
            memoryCached.handle = handle;
            if (regionCache.offer(memoryCached))
            {
                return true;
            }
            else
            {
                memoryCached.recycleHandler.recycle(memoryCached);
                return false;
            }
        }
        else
        {
            return false;
        }
    }
}
