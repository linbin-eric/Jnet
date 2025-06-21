package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.util.ChannelConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public abstract class StorageSegment
{
    protected       Object                   memory;
    // 当是堆外内存的时候才会有值，0是非法值，不应该使用
    protected       long                     nativeAddress;
    protected       int                      capacity;
    protected       int                      offset;
    protected       LeakDetecter.LeakTracker watch;
    @Setter
    protected final BufferAllocator          allocator;
    protected final AtomicInteger            refCount;

    public StorageSegment(BufferAllocator allocator)
    {
        this.allocator = allocator;
        refCount       = new AtomicInteger(0);
    }

    public void init(Object memory, long nativeAddress, int offset, int capacity)
    {
        this.memory        = memory;
        this.nativeAddress = nativeAddress;
        this.offset        = offset;
        this.capacity      = capacity;
        //在初始化的时候，一个内存段的引用次数必须是 0.其他为非法值，出现非法值意味着框架本身出现问题。
        int refCount = getRefCount();
        if (refCount == 0)
        {
            watch = ChannelConfig.IoBufferLeakDetected.watch(this, 9);
        }
        else
        {
            throw new IllegalStateException();
        }
    }

    public int getRefCount()
    {
        return refCount.get();
    }

    public int addRefCount()
    {
        return refCount.incrementAndGet();
    }

    public int decrRefCount()
    {
        return refCount.decrementAndGet();
    }

    public void free()
    {
        int left = decrRefCount();
        if (left > 0)
        {
            return;
        }
        free0();
    }

    protected void free0()
    {
        watch.close();
        watch         = null;
        memory        = null;
        nativeAddress = 0;
        offset        = 0;
        capacity      = 0;
        allocator.cycleStorageSegmentInstance(this);
    }

    public abstract StorageSegment makeNewSegment(int newCapacity, BufferType bufferType);


    public void addInvokeTrace(int skip, int limit)
    {
        watch.addInvokeTrace(skip, limit);
    }
}
