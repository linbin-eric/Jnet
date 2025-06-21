package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Getter;
import lombok.Setter;

public class PooledBuffer extends BasicBuffer
{
    @Getter
    @Setter
    protected RecycleHandler recycleHandler;

    public PooledBuffer(BufferType bufferType, PooledBufferAllocator allocator)
    {
        super(bufferType, allocator);
    }
}
