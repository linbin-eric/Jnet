package com.jfirer.jnet.common.buffer.allocator.impl;

import com.jfirer.baseutil.concurrent.CycleArray;
import com.jfirer.baseutil.concurrent.IndexReadCycleArray;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.MathUtil;
import lombok.Data;

@Data
public class PipelineBufferAllocator implements BufferAllocator
{
    private CycleArray<IoBuffer>       bufferCycleArray;
    private CycleArray<StorageSegment> storageSegmentCycleArray;

    public PipelineBufferAllocator(int cachedNum)
    {
        cachedNum                = MathUtil.normalizeSize(cachedNum);
        bufferCycleArray         = new IndexReadCycleArray<>(cachedNum);
        storageSegmentCycleArray = new IndexReadCycleArray<>(cachedNum);
    }

    @Override
    public IoBuffer ioBuffer(int initializeCapacity)
    {
        return null;
    }

    @Override
    public String name()
    {
        return "";
    }

    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
    }

    @Override
    public void cycleStorageSegmentInstance(StorageSegment storageSegment)
    {
    }
}
