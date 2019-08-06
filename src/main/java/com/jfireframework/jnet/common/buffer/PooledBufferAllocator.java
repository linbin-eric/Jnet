package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.RecycleHandler;
import com.jfireframework.jnet.common.recycler.Recycler;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;
import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.SystemPropertyUtil;

public class PooledBufferAllocator implements BufferAllocator
{
    public static final boolean               USE_CACHE_FOR_ALL_THREAD;
    public static final int                   TINY_CACHE_NUM;
    public static final int                   SMALL_CACHE_NUM;
    public static final int                   NORMAL_CACHE_NUM;
    public static final int                   MAX_CACHEED_BUFFER_CAPACITY;
    public static final int                   PAGESIZE;
    public static final int                   PAGESIZE_SHIFT;
    public static final int                   MAXLEVEL;
    public static final int                   NUM_HEAP_ARENA;
    public static final int                   NUM_DIRECT_ARENA;
    public static final boolean               PREFER_DIRECT;
    public static final PooledBufferAllocator DEFAULT;

    static
    {
        USE_CACHE_FOR_ALL_THREAD = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.useCacheForAllThread", true);
        TINY_CACHE_NUM = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.tinyCacheNum", 256);
        SMALL_CACHE_NUM = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.smallCacheNum", 128);
        NORMAL_CACHE_NUM = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.normalCacheNum", 64);
        PAGESIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
        PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
        MAXLEVEL = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
        NUM_HEAP_ARENA = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numHeapArena", Runtime.getRuntime().availableProcessors());
        NUM_DIRECT_ARENA = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numDirectArena", Runtime.getRuntime().availableProcessors());
        MAX_CACHEED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxCachedBufferCapacity", 32 * 1024);
        PREFER_DIRECT = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);
        DEFAULT = new PooledBufferAllocator(PAGESIZE, MAXLEVEL, NUM_HEAP_ARENA, NUM_DIRECT_ARENA, MAX_CACHEED_BUFFER_CAPACITY, TINY_CACHE_NUM, SMALL_CACHE_NUM, NORMAL_CACHE_NUM, USE_CACHE_FOR_ALL_THREAD, PREFER_DIRECT, "PooledBufferAllocator_Default");
    }

    private final Recycler<PooledDirectBuffer> directBuffers = new Recycler<PooledDirectBuffer>()
    {

        @Override
        protected PooledDirectBuffer newObject(RecycleHandler handler)
        {
            PooledDirectBuffer buffer = new PooledDirectBuffer();
            buffer.recycleHandler = handler;
            return buffer;
        }
    };
    private final Recycler<PooledHeapBuffer>   heapBuffers   = new Recycler<PooledHeapBuffer>()
    {

        @Override
        protected PooledHeapBuffer newObject(RecycleHandler handler)
        {
            PooledHeapBuffer buffer = new PooledHeapBuffer();
            buffer.recycleHandler = handler;
            return buffer;
        }
    };
    boolean       useCacheForAllThread = true;
    boolean       preferDirect;
    int           maxCachedBufferCapacity;
    int           tinyCacheNum;
    int           smallCacheNum;
    int           normalCacheNum;
    int           pagesize;
    int           pagesizeShift;
    int           maxLevel;
    String        name;
    HeapArena[]   heapArenas;
    DirectArena[] directArenas;
    final         FastThreadLocal<ThreadCache> localCache    = new FastThreadLocal<ThreadCache>()
    {
        @Override
        protected ThreadCache initializeValue()
        {
            HeapArena   leastUseHeapArena   = (HeapArena) leastUseArena(heapArenas);
            DirectArena leastUseDirectArena = (DirectArena) leastUseArena(directArenas);
            Thread      currentThread       = Thread.currentThread();
            if (useCacheForAllThread || currentThread instanceof FastThreadLocalThread)
            {
                ThreadCache cache = new ThreadCache(leastUseHeapArena, leastUseDirectArena, tinyCacheNum, smallCacheNum, normalCacheNum, maxCachedBufferCapacity, pagesizeShift);
                return cache;
            }
            else
            {
                ThreadCache cache = new ThreadCache(leastUseHeapArena, leastUseDirectArena, 0, 0, 0, 0, pagesizeShift);
                return cache;
            }
        }

        ;
    };

    public PooledBufferAllocator(String name)
    {
        this(PAGESIZE, MAXLEVEL, NUM_HEAP_ARENA, NUM_DIRECT_ARENA, MAX_CACHEED_BUFFER_CAPACITY, TINY_CACHE_NUM, SMALL_CACHE_NUM, NORMAL_CACHE_NUM, USE_CACHE_FOR_ALL_THREAD, PREFER_DIRECT, name);
    }

    public PooledBufferAllocator(int pagesize, int maxLevel, int numHeapArenas, int numDirectArenas, //
                                 int maxCachedBufferCapacity, int tinyCacheSize, int smallCacheSize, int normalCacheSize, //
                                 boolean useCacheForAllThread, String name)
    {
        this(pagesize, maxLevel, numHeapArenas, numDirectArenas, maxCachedBufferCapacity, tinyCacheSize, smallCacheSize, normalCacheSize, useCacheForAllThread, true, name);
    }

    public PooledBufferAllocator(int pagesize, int maxLevel, int numHeapArenas, int numDirectArenas, //
                                 int maxCachedBufferCapacity, int tinyCacheSize, int smallCacheSize, int normalCacheSize, //
                                 boolean useCacheForAllThread, boolean preferDirect, String name)
    {
        this.maxCachedBufferCapacity = maxCachedBufferCapacity;
        this.tinyCacheNum = tinyCacheSize;
        this.smallCacheNum = smallCacheSize;
        this.normalCacheNum = normalCacheSize;
        this.useCacheForAllThread = useCacheForAllThread;
        this.pagesize = pagesize;
        this.maxLevel = maxLevel;
        this.preferDirect = preferDirect;
        pagesizeShift = MathUtil.log2(pagesize);
        int subpageOverflowMask = ~(pagesize - 1);
        heapArenas = new HeapArena[numHeapArenas];
        this.name = name;
        for (int i = 0; i < numHeapArenas; i++)
        {
            heapArenas[i] = new HeapArena(this, maxLevel, pagesize, pagesizeShift, subpageOverflowMask, "HeapArena-" + i);
        }
        directArenas = new DirectArena[numDirectArenas];
        for (int i = 0; i < numDirectArenas; i++)
        {
            directArenas[i] = new DirectArena(this, maxLevel, pagesize, pagesizeShift, subpageOverflowMask, "DirectArena-" + i);
        }
    }

    private Arena<?> leastUseArena(Arena<?>[] arenas)
    {
        Arena<?> leastUseArena = arenas[0];
        for (Arena<?> each : arenas)
        {
            if (each.numThreadCaches.get() < leastUseArena.numThreadCaches.get())
            {
                leastUseArena = each;
            }
        }
        return leastUseArena;
    }

    public ThreadCache threadCache()
    {
        return localCache.get();
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        ThreadCache      threadCache = localCache.get();
        HeapArena        heapArena   = threadCache.heapArena;
        PooledHeapBuffer buffer      = heapBuffers.get();
        heapArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
        return buffer;
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        ThreadCache        threadCache = localCache.get();
        DirectArena        directArena = threadCache.directArena;
        PooledDirectBuffer buffer      = directBuffers.get();
        directArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
        return buffer;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        if (preferDirect)
        {
            return directBuffer(initializeCapacity);
        }
        else
        {
            return heapBuffer(initializeCapacity);
        }
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
    {
        if (direct)
        {
            return directBuffer(initializeCapacity);
        }
        else
        {
            return heapBuffer(initializeCapacity);
        }
    }

    @Override
    public String name()
    {
        return name;
    }
}
