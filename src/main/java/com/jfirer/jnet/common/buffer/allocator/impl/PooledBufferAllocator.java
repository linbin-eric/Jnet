package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class PooledBufferAllocator implements BufferAllocator
{
    protected final     Recycler<PooledBuffer>         UNSAFE_POOL          = new Recycler<>(() -> new PooledBuffer(BufferType.UNSAFE, this), PooledBuffer::setRecycleHandler);
    protected final     Recycler<PooledBuffer>         HEAP_POOL            = new Recycler<>(() -> new PooledBuffer(BufferType.HEAP, this), PooledBuffer::setRecycleHandler);
    protected final     Recycler<PooledStorageSegment> STORAGE_SEGMENT_POOL = new Recycler<>(() -> new PooledStorageSegment(this), PooledStorageSegment::setRecycleHandler);
    public static final int                            PAGESIZE;
    public static final int                            PAGESIZE_SHIFT;
    public static final int                            MAXLEVEL;
    public static final int                            NUM_OF_ARENA;
    public static final boolean                        PREFER_DIRECT;
    public static final PooledBufferAllocator          DEFAULT;

    static
    {
        PAGESIZE       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
        PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
        MAXLEVEL       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
        NUM_OF_ARENA   = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfArena", Math.min(Runtime.getRuntime().availableProcessors(), 8));
        PREFER_DIRECT  = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);
        DEFAULT        = new PooledBufferAllocator(PAGESIZE, MAXLEVEL, NUM_OF_ARENA, PREFER_DIRECT, "PooledBufferAllocator_Default");
    }

    protected       int                    pagesize;
    protected       int                    maxLevel;
    protected       String                 name;
    protected       ArenaUseCount[]        arenaUseCount;
    protected final boolean                preferDirect;
    protected final FastThreadLocal<Arena> arenaFastThreadLocal = FastThreadLocal.withInitializeValue(() -> leastUseArena());

    public PooledBufferAllocator(String name)
    {
        this(PAGESIZE, MAXLEVEL, NUM_OF_ARENA, PREFER_DIRECT, name);
    }

    public PooledBufferAllocator(String name, boolean preferDirect)
    {
        this(PAGESIZE, MAXLEVEL, NUM_OF_ARENA, preferDirect, name);
    }

    public PooledBufferAllocator(int pagesize, int maxLevel, int numOfArena, boolean preferDirect, String name)
    {
        this.pagesize     = pagesize;
        this.maxLevel     = maxLevel;
        this.preferDirect = preferDirect;
        arenaUseCount     = new ArenaUseCount[numOfArena];
        this.name         = name;
        for (int i = 0; i < numOfArena; i++)
        {
            arenaUseCount[i] = new ArenaUseCount(new AtomicInteger(0), new Arena(maxLevel, pagesize, "Arena-" + i, preferDirect ? BufferType.UNSAFE : BufferType.HEAP));
        }
    }

    public int pagesize()
    {
        return pagesize;
    }

    public int maxLevel()
    {
        return maxLevel;
    }

    private Arena leastUseArena()
    {
        ArenaUseCount leastUseArena = arenaUseCount[0];
        for (ArenaUseCount each : arenaUseCount)
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
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        PooledStorageSegment storageSegment = (PooledStorageSegment) storageSegmentInstance();
        arenaFastThreadLocal.get().allocate(initializeCapacity, storageSegment);
        PooledBuffer pooledBuffer = (PooledBuffer) bufferInstance();
        pooledBuffer.init(storageSegment);
        return pooledBuffer;
    }

    public Arena currentArena()
    {
        return arenaFastThreadLocal.get();
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public IoBuffer bufferInstance()
    {
        if (preferDirect)
        {
            return UNSAFE_POOL.get();
        }
        else
        {
            return HEAP_POOL.get();
        }
    }

    @Override
    public StorageSegment storageSegmentInstance()
    {
        return STORAGE_SEGMENT_POOL.get();
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        ((PooledBuffer) buffer).getRecycleHandler().recycle(buffer);
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
        ((PooledStorageSegment) storageSegment).getRecycleHandler().recycle(storageSegment);
    }

    public void capacityStat(CapacityStat stat)
    {
        for (ArenaUseCount each : arenaUseCount)
        {
            each.arena.capacityStat(stat);
        }
    }

    record ArenaUseCount(AtomicInteger use, Arena arena)
    {
    }
}
