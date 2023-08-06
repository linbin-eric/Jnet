package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.recycler.Recycler;
import lombok.Data;

@Data
public class CachedStorageSegment extends PooledStorageSegment
{
    public static final Recycler<CachedStorageSegment> POOL = new Recycler<>(CachedStorageSegment::new, StorageSegment::setRecycleHandler);
    private             ThreadCache                    threadCache;
    private             int                            bitMapIndex;
    private             int                            regionIndex;

    @Override
    protected void free0()
    {
        if (threadCache != null)
        {
            //需要执行回收到 ThreadCache 的工作，不将内存空间回收到 Arena 中。
            threadCache.free(regionIndex, bitMapIndex);
        }
        else
        {
            super.free0();
        }
    }

    @Override
    public StorageSegment makeNewSegment(int newCapacity, BufferType bufferType)
    {
        if (threadCache != null)
        {
            return threadCache.allocate(newCapacity);
        }
        else
        {
            return super.makeNewSegment(newCapacity, bufferType);
        }
    }
}
