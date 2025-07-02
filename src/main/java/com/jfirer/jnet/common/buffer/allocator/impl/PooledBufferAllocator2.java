package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.baseutil.concurrent.BitmapObjectPool;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer2;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.SystemPropertyUtil;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

public class PooledBufferAllocator2 implements BufferAllocator
{
    public static final int     PAGESIZE;
    public static final int     PAGESIZE_SHIFT;
    public static final int     MAXLEVEL;
    public static final int     NUM_OF_ARENA;
    public static final boolean PREFER_DIRECT;

    static
    {
        PAGESIZE       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.pageSize", 8192);
        PAGESIZE_SHIFT = MathUtil.log2(PAGESIZE);
        MAXLEVEL       = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.maxLevel", 11);
        NUM_OF_ARENA   = SystemPropertyUtil.getInt("io.jnet.PooledBufferAllocator.numOfArena", Math.min(Runtime.getRuntime().availableProcessors(), 8));
        PREFER_DIRECT  = SystemPropertyUtil.getBoolean("io.jnet.PooledBufferAllocator.preferDirect", true);
    }

    private              BitmapObjectPool<PooledBuffer2> bufferPool;
    private final        boolean                         preferDirect;
    @Getter
    private final        Arena                           arena;
    private static final Arena[]                         HEAP_ARENAS;
    private static final Arena[]                         UNSAFE_ARENAS;
    private static final AtomicInteger                   ARENA_COUNT         = new AtomicInteger();
    private              boolean                         reUseBufferInstance = true;

    static
    {
        HEAP_ARENAS = new Arena[PooledBufferAllocator2.NUM_OF_ARENA];
        for (int i = 0; i < HEAP_ARENAS.length; i++)
        {
            HEAP_ARENAS[i] = new Arena(PooledBufferAllocator2.MAXLEVEL, PooledBufferAllocator2.PAGESIZE, "PipelineBufferAllocator_heap_" + i, BufferType.HEAP);
        }
        UNSAFE_ARENAS = new Arena[NUM_OF_ARENA];
        for (int i = 0; i < UNSAFE_ARENAS.length; i++)
        {
            UNSAFE_ARENAS[i] = new Arena(MAXLEVEL, PAGESIZE, "PipelineBufferAllocator_unsafe_" + i, BufferType.UNSAFE);
        }
    }

    public PooledBufferAllocator2(int cachedNum, boolean preferDirect, Arena arena)
    {
        this.preferDirect = preferDirect;
        this.arena        = arena;
        cachedNum         = MathUtil.normalizeSize(cachedNum);
        bufferPool        = preferDirect ? new BitmapObjectPool<>(i -> new PooledBuffer2(BufferType.UNSAFE, this, i), cachedNum)//
                : new BitmapObjectPool<>(i -> new PooledBuffer2(BufferType.HEAP, this, i), cachedNum);
    }

    @Override
    public IoBuffer allocate(int initializeCapacity)
    {
        PooledBuffer2 buffer2 = bufferInstance();
        arena.allocate(initializeCapacity, buffer2);
        buffer2.initRefCnt();
        return buffer2;
    }

    @Override
    public void reAllocate(int initializeCapacity, IoBuffer buffer2)
    {
        arena.allocate(initializeCapacity, ((PooledBuffer2) buffer2));
    }

    @Override
    public String name()
    {
        return "";
    }

    @Override
    public PooledBuffer2 bufferInstance()
    {
        if (reUseBufferInstance)
        {
            PooledBuffer2 acquire = bufferPool.acquire();
            if (acquire == null)
            {
                acquire = new PooledBuffer2(preferDirect ? BufferType.UNSAFE : BufferType.HEAP, this, -1);
            }
            return acquire;
        }
        else
        {
            return new PooledBuffer2(preferDirect ? BufferType.UNSAFE : BufferType.HEAP, this, -1);
        }
    }

    @Override
    public StorageSegment storageSegmentInstance()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        PooledBuffer2 pipelineBuffer = (PooledBuffer2) buffer;
        int           bitmapIndex    = pipelineBuffer.getBitmapIndex();
        if (bitmapIndex == -1)
        {
            ;
        }
        else
        {
            bufferPool.release(bitmapIndex);
        }
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
        throw new UnsupportedOperationException();
    }

    public void capacityStat(CapacityStat stat)
    {
        arena.capacityStat(stat);
    }

    public static Arena getArena(boolean preferDirect)
    {
        return preferDirect ? UNSAFE_ARENAS[ARENA_COUNT.getAndIncrement() % UNSAFE_ARENAS.length] : HEAP_ARENAS[ARENA_COUNT.getAndIncrement() % HEAP_ARENAS.length];
    }
}
