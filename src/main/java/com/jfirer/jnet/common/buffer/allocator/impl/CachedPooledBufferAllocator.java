package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePooledBuffer;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.util.concurrent.atomic.LongAdder;

public class CachedPooledBufferAllocator extends PooledBufferAllocator
{
    protected final     FastThreadLocal<ThreadCache>    THREAD_CACHE_FOR_DIRECT;
    protected final     FastThreadLocal<ThreadCache>    THREAD_CACHE_FOR_HEAP;
    public static final int                             NUM_OF_CACHE;
    public static final int                             MAX_CACHED_BUFFER_CAPACITY;
    public static       CachedPooledBufferAllocator     DEFAULT                 = new CachedPooledBufferAllocator("CachedPooledBufferAllocator_default");
    protected           Recycler<CacheablePooledBuffer> DIRECT_BUFFER_ALLOCATOR = new Recycler<>(() -> new CacheablePooledBuffer(BufferType.UNSAFE), AbstractBuffer::setRecycleHandler);
    protected           Recycler<CacheablePooledBuffer> HEAP_BUFFER_ALLOCATOR   = new Recycler<>(() -> new CacheablePooledBuffer(BufferType.HEAP), AbstractBuffer::setRecycleHandler);
    static
    {
        NUM_OF_CACHE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfCache", 512);
        MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxCachedBufferCapacity", 32 * 1024);
    }
    public CachedPooledBufferAllocator(String name)
    {
        super(name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, directArenaFastThreadLocal.get()));
        THREAD_CACHE_FOR_HEAP = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, heapArenaFastThreadLocal.get()));
    }

    public CachedPooledBufferAllocator(int pagesize, int maxLevel, int numOfArena, boolean preferDirect, String name, int numOfCached, int maxCachedBufferCapacity)
    {
        super(pagesize, maxLevel, numOfArena, preferDirect, name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache(numOfCached, maxCachedBufferCapacity, directArenaFastThreadLocal.get()));
        THREAD_CACHE_FOR_HEAP = FastThreadLocal.withInitializeValue(() -> new ThreadCache(numOfCached, maxCachedBufferCapacity, heapArenaFastThreadLocal.get()));
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        CacheablePooledBuffer buffer      = HEAP_BUFFER_ALLOCATOR.get();
        ThreadCache           threadCache = THREAD_CACHE_FOR_HEAP.get();
        threadCache.allocate(initializeCapacity, buffer);
        return buffer;
    }

    @Override
    public IoBuffer unsafeBuffer(int initializeCapacity)
    {
        CacheablePooledBuffer buffer      = DIRECT_BUFFER_ALLOCATOR.get();
        ThreadCache           threadCache = THREAD_CACHE_FOR_DIRECT.get();
        threadCache.allocate(initializeCapacity, buffer);
        return buffer;
    }

    public LongAdder success = new LongAdder();
    public LongAdder fail    = new LongAdder();
}
