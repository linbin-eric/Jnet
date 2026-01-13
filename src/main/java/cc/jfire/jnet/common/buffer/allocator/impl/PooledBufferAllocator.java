package cc.jfire.jnet.common.buffer.allocator.impl;

import cc.jfire.baseutil.concurrent.BitmapObjectPool;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.arena.Arena;
import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.buffer.buffer.impl.PooledBuffer;
import cc.jfire.jnet.common.util.CapacityStat;
import cc.jfire.jnet.common.util.MathUtil;
import cc.jfire.jnet.common.util.SystemPropertyUtil;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

public class PooledBufferAllocator implements BufferAllocator
{
    public static final int     PAGESIZE;
    public static final int     PAGESIZE_SHIFT;
    public static final int     MAXLEVEL;
    public static final int     NUM_OF_ARENA;
    public static final boolean PREFER_DIRECT;
    private static final Arena[]                        HEAP_ARENAS;
    private static final Arena[]                        UNSAFE_ARENAS;
    private static final AtomicInteger                  ARENA_COUNT         = new AtomicInteger();

    static
    {
        PAGESIZE       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
        PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
        MAXLEVEL       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
        NUM_OF_ARENA   = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfArena", Math.min(Runtime.getRuntime().availableProcessors(), 8));
        PREFER_DIRECT  = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);
    }

    static
    {
        HEAP_ARENAS = new Arena[PooledBufferAllocator.NUM_OF_ARENA];
        for (int i = 0; i < HEAP_ARENAS.length; i++)
        {
            HEAP_ARENAS[i] = new Arena(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, "PipelineBufferAllocator_heap_" + i, BufferType.HEAP);
        }
        UNSAFE_ARENAS = new Arena[NUM_OF_ARENA];
        for (int i = 0; i < UNSAFE_ARENAS.length; i++)
        {
            UNSAFE_ARENAS[i] = new Arena(MAXLEVEL, PAGESIZE, "PipelineBufferAllocator_unsafe_" + i, BufferType.UNSAFE);
        }
    }

    private final        boolean                        preferDirect;
    @Getter
    private final Arena                          arena;
    private final BitmapObjectPool<PooledBuffer> bufferPool;
    private final boolean                        reUseBufferInstance = false;

    public PooledBufferAllocator(int cachedNum, boolean preferDirect, Arena arena)
    {
        this.preferDirect = preferDirect;
        this.arena        = arena;
        cachedNum         = MathUtil.normalizeSize(cachedNum);
        bufferPool        = preferDirect ? new BitmapObjectPool<>(i -> new PooledBuffer(BufferType.UNSAFE, this, i), cachedNum)//
                : new BitmapObjectPool<>(i -> new PooledBuffer(BufferType.HEAP, this, i), cachedNum);
    }

    public static Arena getArena(boolean preferDirect)
    {
        return preferDirect ? UNSAFE_ARENAS[ARENA_COUNT.getAndIncrement() % UNSAFE_ARENAS.length] : HEAP_ARENAS[ARENA_COUNT.getAndIncrement() % HEAP_ARENAS.length];
    }

    @Override
    public IoBuffer allocate(int initializeCapacity)
    {
        PooledBuffer buffer2 = bufferInstance();
        arena.allocate(initializeCapacity, buffer2);
        buffer2.initRefCnt();
        return buffer2;
    }

    @Override
    public void reAllocate(int initializeCapacity, IoBuffer buffer2)
    {
        arena.allocate(initializeCapacity, ((PooledBuffer) buffer2));
    }

    @Override
    public String name()
    {
        return "";
    }

    @Override
    public PooledBuffer bufferInstance()
    {
        if (reUseBufferInstance)
        {
            PooledBuffer acquire = bufferPool.acquire();
            if (acquire == null)
            {
                acquire = new PooledBuffer(preferDirect ? BufferType.UNSAFE : BufferType.HEAP, this, -1);
            }
            return acquire;
        }
        else
        {
            return new PooledBuffer(preferDirect ? BufferType.UNSAFE : BufferType.HEAP, this, -1);
        }
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        PooledBuffer pipelineBuffer = (PooledBuffer) buffer;
        int          bitmapIndex    = pipelineBuffer.getBitmapIndex();
        if (bitmapIndex == -1)
        {
            ;
        }
        else
        {
            bufferPool.release(bitmapIndex);
        }
    }

    public void capacityStat(CapacityStat stat)
    {
        arena.capacityStat(stat);
    }
}
