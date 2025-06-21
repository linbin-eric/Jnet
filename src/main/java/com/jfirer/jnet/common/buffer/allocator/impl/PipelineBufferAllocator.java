package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.baseutil.concurrent.BitmapObjectPool;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PipelineBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PipelineStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.MathUtil;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class PipelineBufferAllocator implements BufferAllocator
{
    private              BitmapObjectPool<PipelineBuffer>         bufferPool;
    private              BitmapObjectPool<PipelineStorageSegment> storageSegmentPool;
    private final        boolean                                  preferDirect;
    private final        Arena                                    arena;
    private static final Arena[]                                  HEAP_ARENAS;
    private static final Arena[]                                  UNSAFE_ARENAS;
    private static final AtomicInteger                            ARENA_COUNT = new AtomicInteger();

    static
    {
        HEAP_ARENAS = new Arena[PooledBufferAllocator.NUM_OF_ARENA];
        for (int i = 0; i < HEAP_ARENAS.length; i++)
        {
            HEAP_ARENAS[i] = new Arena(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, "PipelineBufferAllocator_heap_" + i, BufferType.HEAP);
        }
        UNSAFE_ARENAS = new Arena[PooledBufferAllocator.NUM_OF_ARENA];
        for (int i = 0; i < UNSAFE_ARENAS.length; i++)
        {
            UNSAFE_ARENAS[i] = new Arena(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, "PipelineBufferAllocator_unsafe_" + i, BufferType.UNSAFE);
        }
    }

    public PipelineBufferAllocator(int cachedNum, boolean preferDirect, Arena arena)
    {
        this.preferDirect  = preferDirect;
        this.arena         = arena;
        cachedNum          = MathUtil.normalizeSize(cachedNum);
        bufferPool         = preferDirect ? new BitmapObjectPool<>(i -> new PipelineBuffer(BufferType.UNSAFE, this, i), cachedNum)//
                : new BitmapObjectPool<>(i -> new PipelineBuffer(BufferType.HEAP, this, i), cachedNum);
        storageSegmentPool = preferDirect ? new BitmapObjectPool<>(i -> new PipelineStorageSegment(this, i), cachedNum)//
                : new BitmapObjectPool<>(i -> new PipelineStorageSegment(this, i), cachedNum);
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        PipelineStorageSegment storageSegment = (PipelineStorageSegment) storageSegmentInstance();
        arena.allocate(initializeCapacity, storageSegment);
        PipelineBuffer buffer = (PipelineBuffer) bufferInstance();
        buffer.init(storageSegment);
        return buffer;
    }

    @Override
    public String name()
    {
        return "";
    }

    @Override
    public UnPooledBuffer bufferInstance()
    {
        PipelineBuffer acquire = bufferPool.acquire();
        if (acquire == null)
        {
            acquire = new PipelineBuffer(preferDirect ? BufferType.UNSAFE : BufferType.HEAP, this, -1);
        }
        return acquire;
    }

    @Override
    public StorageSegment storageSegmentInstance()
    {
        PipelineStorageSegment acquire = storageSegmentPool.acquire();
        if (acquire == null)
        {
            acquire = new PipelineStorageSegment(this, -1);
        }
        return acquire;
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        PipelineBuffer pipelineBuffer = (PipelineBuffer) buffer;
        int            bitmapIndex    = pipelineBuffer.getBitmapIndex();
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
        PipelineStorageSegment pipelineStorageSegment = (PipelineStorageSegment) storageSegment;
        int                    bitmapIndex            = pipelineStorageSegment.getBitmapIndex();
        if (bitmapIndex == -1)
        {
            ;
        }
        else
        {
            storageSegmentPool.release(bitmapIndex);
        }
    }

    public static Arena getArena(boolean preferDirect)
    {
        return preferDirect ? UNSAFE_ARENAS[ARENA_COUNT.getAndIncrement() % UNSAFE_ARENAS.length] : HEAP_ARENAS[ARENA_COUNT.getAndIncrement() % HEAP_ARENAS.length];
    }
}
