package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.impl.AbstractArena;
import com.jfirer.jnet.common.buffer.arena.impl.DirectArena;
import com.jfirer.jnet.common.buffer.arena.impl.HeapArena;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledHeapBuffer;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.util.concurrent.atomic.AtomicInteger;

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
    boolean         useCacheForAllThread = true;
    boolean         preferDirect;
    int             maxCachedBufferCapacity;
    int             tinyCacheNum;
    int             smallCacheNum;
    int             normalCacheNum;
    int             pagesize;
    int             pagesizeShift;
    int             maxLevel;
    String          name;
    ArenaUseCount[] heapArenaUseCount;
    ArenaUseCount[] directArenaUseCount;

    record ArenaUseCount(AtomicInteger use, Arena<?> arena) {}

    protected final FastThreadLocal<DirectArena> directArenaFastThreadLocal = FastThreadLocal.withInitializeValue(() -> (DirectArena) leastUseArena(directArenaUseCount));
    protected final FastThreadLocal<HeapArena>   heapArenaFastThreadLocal   = FastThreadLocal.withInitializeValue(() -> (HeapArena) leastUseArena(heapArenaUseCount));

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
        heapArenaUseCount = new ArenaUseCount[numHeapArenas];
        this.name = name;
        for (int i = 0; i < numHeapArenas; i++)
        {
            heapArenaUseCount[i] = new ArenaUseCount(new AtomicInteger(0), new HeapArena(maxLevel, pagesize, "HeapArena-" + i));
        }
        directArenaUseCount = new ArenaUseCount[numDirectArenas];
        for (int i = 0; i < numDirectArenas; i++)
        {
            directArenaUseCount[i] = new ArenaUseCount(new AtomicInteger(0), new DirectArena(maxLevel, pagesize, "DirectArena-" + i));
        }
    }

    private Arena<?> leastUseArena(ArenaUseCount[] arenaUseCounts)
    {
        ArenaUseCount leastUseArena = arenaUseCounts[0];
        for (ArenaUseCount each : arenaUseCounts)
        {
            if (each.use.get() < leastUseArena.use.get())
            {
                leastUseArena = each;
            }
        }
        leastUseArena.use.incrementAndGet();
        return leastUseArena.arena();
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        return PooledHeapBuffer.allocate(heapArenaFastThreadLocal.get(), initializeCapacity);
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        return PooledDirectBuffer.allocate(directArenaFastThreadLocal.get(), initializeCapacity);
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

    public Arena<?> currentArena(boolean preferDirect)
    {
        if (preferDirect)
        {
            return directArenaFastThreadLocal.get();
        }
        else
        {
            return heapArenaFastThreadLocal.get();
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

    public void heapCapacityStat(CapacityStat stat)
    {
        for (ArenaUseCount each : heapArenaUseCount)
        {
            ((AbstractArena) each.arena).capacityStat(stat);
        }
    }

    public void directCapacityStat(CapacityStat stat)
    {
        for (ArenaUseCount each : directArenaUseCount)
        {
            ((AbstractArena) each.arena).capacityStat(stat);
        }
    }
}
