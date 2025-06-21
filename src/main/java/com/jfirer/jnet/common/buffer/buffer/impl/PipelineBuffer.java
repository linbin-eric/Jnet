package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import lombok.Getter;

public class PipelineBuffer extends UnPooledBuffer
{
    @Getter
    private final int bitmapIndex;

    public PipelineBuffer(BufferType bufferType, BufferAllocator allocator, int bitmapIndex)
    {
        super(bufferType, allocator);
        this.bitmapIndex = bitmapIndex;
    }
}
