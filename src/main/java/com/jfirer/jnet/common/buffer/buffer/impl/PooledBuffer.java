package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;

public class PooledBuffer extends AbstractBuffer
{
    protected Arena         arena;
    protected ChunkListNode chunkListNode;
    protected long          handle;

    public PooledBuffer(BufferType bufferType) {super(bufferType);}

    public void init(Arena arena, ChunkListNode chunkListNode, int capacity, int offset, long handle)
    {
        this.arena = arena;
        this.chunkListNode = chunkListNode;
        this.handle = handle;
        init(chunkListNode.memory(), capacity, offset, chunkListNode.directChunkAddress());
    }

    public Chunk chunk()
    {
        return chunkListNode;
    }

    public long handle()
    {
        return handle;
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        arena.reAllocate(chunkListNode, this, reqCapacity);
    }

    @Override
    protected void free0(int capacity)
    {
        arena.free(chunkListNode, handle, capacity);
    }
}
