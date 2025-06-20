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
    IoBuffer ioBuffer(int initializeCapacity);

    String name();

    void cycleBufferInstance(IoBuffer buffer);

    void cycleStorageSegmentInstance(StorageSegment storageSegment);
}
