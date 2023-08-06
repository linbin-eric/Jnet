package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.PlatFormFunction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class StorageSegment extends AtomicInteger
{
    public static final Recycler<StorageSegment> POOL = new Recycler<>(StorageSegment::new, StorageSegment::setRecycleHandler);
    protected           Object                   memory;
    // 当是堆外内存的时候才会有值，0是非法值，不应该使用
    protected           long                     nativeAddress;
    protected           int                      capacity;
    protected           int                      offset;
    protected           LeakDetecter.LeakTracker watch;
    @Setter
    @Getter(AccessLevel.NONE)
    protected           RecycleHandler           recycleHandler;

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
        return get();
    }

    public int addRefCount()
    {
        return incrementAndGet();
    }

    public int decrRefCount()
    {
        return decrementAndGet();
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
        if (recycleHandler != null)
        {
            recycleHandler.recycle(this);
        }
    }

    public StorageSegment makeNewSegment(int newCapacity, BufferType bufferType)
    {
        StorageSegment newSegment = StorageSegment.POOL.get();
        newCapacity = newCapacity > capacity * 2 ? newCapacity : 2 * capacity;
        switch (bufferType)
        {
            case HEAP ->
            {
                newSegment.init(new byte[newCapacity], 0, 0, newCapacity);
            }
            case DIRECT, MEMORY ->
            {
                throw new UnsupportedOperationException();
            }
            case UNSAFE ->
            {
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(newCapacity);
                newSegment.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, newCapacity);
            }
        }
        return newSegment;
    }
}
