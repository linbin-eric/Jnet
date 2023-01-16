package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.arena.impl.DirectArena;
import com.jfirer.jnet.common.buffer.arena.impl.HeapArena;
import com.jfirer.jnet.common.buffer.buffer.CachedPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CachedPooledDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CachedPooledHeapBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.nio.ByteBuffer;

public class CachedPooledBufferAllocator extends PooledBufferAllocator
{
    protected final     FastThreadLocal<ThreadCache<ByteBuffer>> THREAD_CACHE_FOR_DIRECT;
    protected final     FastThreadLocal<ThreadCache<byte[]>>     THREAD_CACHE_FOR_HEAP;
    public static final int                                      NUM_OF_CACHE;
    public static final int                                      MAX_CACHED_BUFFER_CAPACITY;
    public static       CachedPooledBufferAllocator              DEFAULT = new CachedPooledBufferAllocator("CachedPooledBufferAllocator_default");
    static
    {
        NUM_OF_CACHE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfCache", 128);
        MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxCachedBufferCapacity", 32 * 1024);
    }
    public CachedPooledBufferAllocator(String name)
    {
        super(name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache<>(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY));
        THREAD_CACHE_FOR_HEAP = FastThreadLocal.withInitializeValue(() -> new ThreadCache<>(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY));
    }

    public CachedPooledBufferAllocator(int pagesize, int maxLevel, int numOfArena, boolean preferDirect, String name, int numOfCached, int maxCachedBufferCapacity)
    {
        super(pagesize, maxLevel, numOfArena, preferDirect, name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache<>(numOfCached, maxCachedBufferCapacity));
        THREAD_CACHE_FOR_HEAP = FastThreadLocal.withInitializeValue(() -> new ThreadCache<>(numOfCached, maxCachedBufferCapacity));
    }

    protected final Recycler<CachedPooledDirectBuffer> CACHED_POOLED_DIRECT_BUFFER_ALLOCATOR = new Recycler<>(function -> {
        CachedPooledDirectBuffer                 buffer         = new CachedPooledDirectBuffer();
        RecycleHandler<CachedPooledDirectBuffer> recycleHandler = function.apply(buffer);
        buffer.setRecycleHandler(recycleHandler);
        return buffer;
    });
    protected final Recycler<CachedPooledHeapBuffer>   cached_pooled_heap_buffer_allocator   = new Recycler<>(function -> {
        CachedPooledHeapBuffer                 buffer         = new CachedPooledHeapBuffer();
        RecycleHandler<CachedPooledHeapBuffer> recycleHandler = function.apply(buffer);
        buffer.setRecycleHandler(recycleHandler);
        return buffer;
    });

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        CachedPooledBuffer<byte[]> buffer      = cached_pooled_heap_buffer_allocator.get();
        ThreadCache<byte[]>        threadCache = THREAD_CACHE_FOR_HEAP.get();
        if (threadCache.allocate(initializeCapacity, buffer))
        {
            ;
        }
        else
        {
            HeapArena heapArena = heapArenaFastThreadLocal.get();
            heapArena.allocate(initializeCapacity, buffer);
            buffer.setCache(threadCache);
        }
        return buffer;
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        CachedPooledBuffer<ByteBuffer> buffer      = CACHED_POOLED_DIRECT_BUFFER_ALLOCATOR.get();
        ThreadCache<ByteBuffer>        threadCache = THREAD_CACHE_FOR_DIRECT.get();
        if (threadCache.allocate(initializeCapacity, buffer))
        {
            ;
        }
        else
        {
            DirectArena arena = directArenaFastThreadLocal.get();
            arena.allocate(initializeCapacity, buffer);
            buffer.setCache(threadCache);
        }
        return buffer;
    }
}
