package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPooledStorageSegment extends StorageSegment
{
    public UnPooledStorageSegment(BufferAllocator allocator)
    {
        super(allocator);
    }

    @Override
    public StorageSegment makeNewSegment(int newCapacity, BufferType bufferType)
    {
        StorageSegment newSegment = allocator.storageSegmentInstance();
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
