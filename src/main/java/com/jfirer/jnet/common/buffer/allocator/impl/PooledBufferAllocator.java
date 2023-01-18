package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.impl.AbstractArena;
import com.jfirer.jnet.common.buffer.arena.impl.DirectArena;
import com.jfirer.jnet.common.buffer.arena.impl.HeapArena;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableHeapBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableUnsafeBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.SystemPropertyUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class PooledBufferAllocator implements BufferAllocator
{
    public static final int                   PAGESIZE;
    public static final int                   PAGESIZE_SHIFT;
    public static final int                   MAXLEVEL;
    public static final int                   NUM_OF_ARENA;
    public static final boolean               PREFER_DIRECT;
    public static final PooledBufferAllocator DEFAULT;
    static
    {
        PAGESIZE = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
        PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
        MAXLEVEL = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
        NUM_OF_ARENA = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfArena", Runtime.getRuntime().availableProcessors() * 2);
        PREFER_DIRECT = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);
        DEFAULT = new PooledBufferAllocator(PAGESIZE, MAXLEVEL, NUM_OF_ARENA, PREFER_DIRECT, "PooledBufferAllocator_Default");
    }
    protected boolean                                       preferDirect;
    protected int                                           pagesize;
    protected int                                           maxLevel;
    protected String                                        name;
    protected ArenaUseCount[]                               heapArenaUseCount;
    protected ArenaUseCount[]                               directArenaUseCount;
    protected Recycler<CacheablePoolableBuffer<ByteBuffer>> DIRECT_BUFFER_ALLOCATOR = new Recycler<>(function -> {
        CacheablePoolableBuffer<ByteBuffer>                 buffer  = new CacheablePoolableUnsafeBuffer();
        RecycleHandler<CacheablePoolableBuffer<ByteBuffer>> handler = function.apply(buffer);
        buffer.setRecycleHandler(handler);
        return buffer;
    });
    protected Recycler<CacheablePoolableBuffer<byte[]>>     HEAP_BUFFER_ALLOCATOR   = new Recycler<>(function -> {
        CacheablePoolableBuffer<byte[]>                 buffer  = new CacheablePoolableHeapBuffer();
        RecycleHandler<CacheablePoolableBuffer<byte[]>> handler = function.apply(buffer);
        buffer.setRecycleHandler(handler);
        return buffer;
    });

    public int pagesize()
    {
        return pagesize;
    }

    public int maxLevel()
    {
        return maxLevel;
    }

    record ArenaUseCount(AtomicInteger use, Arena<?> arena) {}

    protected final FastThreadLocal<DirectArena> directArenaFastThreadLocal = FastThreadLocal.withInitializeValue(() -> (DirectArena) leastUseArena(directArenaUseCount));
    protected final FastThreadLocal<HeapArena>   heapArenaFastThreadLocal   = FastThreadLocal.withInitializeValue(() -> (HeapArena) leastUseArena(heapArenaUseCount));

    public PooledBufferAllocator(String name)
    {
        this(PAGESIZE, MAXLEVEL, NUM_OF_ARENA, PREFER_DIRECT, name);
    }

    public PooledBufferAllocator(int pagesize, int maxLevel, int numOfArena, boolean preferDirect, String name)
    {
        this.pagesize = pagesize;
        this.maxLevel = maxLevel;
        this.preferDirect = preferDirect;
        heapArenaUseCount = new ArenaUseCount[numOfArena];
        directArenaUseCount = new ArenaUseCount[numOfArena];
        this.name = name;
        for (int i = 0; i < numOfArena; i++)
        {
            heapArenaUseCount[i] = new ArenaUseCount(new AtomicInteger(0), new HeapArena(maxLevel, pagesize, "HeapArena-" + i));
        }
        for (int i = 0; i < numOfArena; i++)
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
        PoolableBuffer<byte[]> pooledHeapBuffer = HEAP_BUFFER_ALLOCATOR.get();
        HeapArena              heapArena        = heapArenaFastThreadLocal.get();
        heapArena.allocate(initializeCapacity, pooledHeapBuffer);
        return pooledHeapBuffer;
    }

    @Override
    public IoBuffer unsafeBuffer(int initializeCapacity)
    {
        PoolableBuffer<ByteBuffer> pooledDirectBuffer = DIRECT_BUFFER_ALLOCATOR.get();
        DirectArena                arena              = directArenaFastThreadLocal.get();
        arena.allocate(initializeCapacity, pooledDirectBuffer);
        return pooledDirectBuffer;
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        if (preferDirect)
        {
            return unsafeBuffer(initializeCapacity);
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
            return unsafeBuffer(initializeCapacity);
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
