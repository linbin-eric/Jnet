package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.ThreadCache;
import lombok.Getter;
import lombok.Setter;

public class CachedStorageSegment extends StorageSegment
{
    @Setter
    @Getter
    private ThreadCache threadCache;
    @Setter
    @Getter
    private int         bitMapIndex;
    @Setter
    @Getter
    private int         regionIndex;

    public CachedStorageSegment(BufferAllocator allocator)
    {
        super(allocator);
    }

    @Override
    protected void free0()
    {
        //需要执行回收到 ThreadCache 的工作，不将内存空间回收到 Arena 中。
        threadCache.free(regionIndex, bitMapIndex);
    }

    @Override
    public StorageSegment makeNewSegment(int newCapacity, BufferType bufferType)
    {
        return threadCache.allocate(newCapacity);
    }
}
