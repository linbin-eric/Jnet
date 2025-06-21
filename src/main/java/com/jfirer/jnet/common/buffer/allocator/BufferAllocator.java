package com.jfirer.jnet.common.buffer.allocator;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;

public interface BufferAllocator
{
    /**
     * 不限定Buffer类型，自行决定
     *
     * @return
     */
    IoBuffer ioBuffer(int initializeCapacity);

    default String name()
    {
        return this.getClass().getName();
    }

    BasicBuffer bufferInstance();

    StorageSegment storageSegmentInstance();

    void cycleBufferInstance(BasicBuffer buffer);

    void cycleStorageSegmentInstance(StorageSegment storageSegment);
}
