package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;

public class PooledBuffer2 extends UnPooledBuffer2
{
    protected Arena         arena;
    protected ChunkListNode chunkListNode;
    protected long          handle;

    public PooledBuffer2(BufferType bufferType, BufferAllocator allocator)
    {
        super(bufferType, allocator);
    }
}
