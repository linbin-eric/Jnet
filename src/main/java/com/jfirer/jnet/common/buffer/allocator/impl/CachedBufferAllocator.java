package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.CachedStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.util.concurrent.atomic.LongAdder;

public class CachedBufferAllocator extends PooledBufferAllocator
{
    public static final int                   NUM_OF_CACHE;
    public static final int                   MAX_CACHED_BUFFER_CAPACITY;
    public static final CachedBufferAllocator DEFAULT = new CachedBufferAllocator("CachedPooledBufferAllocator_default");

    static
    {
        NUM_OF_CACHE               = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfCache", 512);
        MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxCachedBufferCapacity", 32 * 1024);
    }

    protected final FastThreadLocal<ThreadCache> THREAD_CACHE_FOR_DIRECT;
    protected final FastThreadLocal<ThreadCache> THREAD_CACHE_FOR_HEAP;
    public          LongAdder                    success = new LongAdder();
    public          LongAdder                    fail    = new LongAdder();

    public CachedBufferAllocator(String name)
    {
        super(name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, directArenaFastThreadLocal.get(), BufferType.UNSAFE, this));
        THREAD_CACHE_FOR_HEAP   = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, heapArenaFastThreadLocal.get(), BufferType.HEAP, this));
    }

    public CachedBufferAllocator(String name, boolean preferDirect)
    {
        super(name, preferDirect);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, directArenaFastThreadLocal.get(), BufferType.UNSAFE, this));
        THREAD_CACHE_FOR_HEAP   = FastThreadLocal.withInitializeValue(() -> new ThreadCache(NUM_OF_CACHE, MAX_CACHED_BUFFER_CAPACITY, heapArenaFastThreadLocal.get(), BufferType.HEAP, this));
    }

    public CachedBufferAllocator(int pagesize, int maxLevel, int numOfArena, boolean preferDirect, String name, int numOfCached, int maxCachedBufferCapacity)
    {
        super(pagesize, maxLevel, numOfArena, preferDirect, name);
        THREAD_CACHE_FOR_DIRECT = FastThreadLocal.withInitializeValue(() -> new ThreadCache(numOfCached, maxCachedBufferCapacity, directArenaFastThreadLocal.get(), BufferType.UNSAFE, this));
        THREAD_CACHE_FOR_HEAP   = FastThreadLocal.withInitializeValue(() -> new ThreadCache(numOfCached, maxCachedBufferCapacity, heapArenaFastThreadLocal.get(), BufferType.HEAP, this));
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        StorageSegment storageSegment;
        if (preferDirect)
        {
            storageSegment = THREAD_CACHE_FOR_DIRECT.get().allocate(initializeCapacity);
        }
        else
        {
            storageSegment = THREAD_CACHE_FOR_HEAP.get().allocate(initializeCapacity);
        }
        BasicBuffer pooledDirectBuffer = bufferInstance();
        pooledDirectBuffer.init(storageSegment);
        return pooledDirectBuffer;
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
        if (storageSegment instanceof CachedStorageSegment cachedStorageSegment)
        {
            throw new IllegalArgumentException();
        }
        else if (storageSegment instanceof PooledStorageSegment pooledStorageSegment)
        {
            super.cycleStorageSegmentInstance(pooledStorageSegment);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
}
