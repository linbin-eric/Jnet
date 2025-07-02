package com.jfirer.jnet.common.buffer.allocator;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;

public interface BufferAllocator
{
    /**
     * 不限定Buffer类型，自行决定
     *
     * @return
     */
    IoBuffer allocate(int initializeCapacity);

    void reAllocate(int initializeCapacity, IoBuffer buffer2);

    default String name()
    {
        return this.getClass().getName();
    }

    IoBuffer bufferInstance();

    StorageSegment storageSegmentInstance();

    void cycleBufferInstance(IoBuffer buffer);

    void cycleStorageSegmentInstance(StorageSegment storageSegment);
}
