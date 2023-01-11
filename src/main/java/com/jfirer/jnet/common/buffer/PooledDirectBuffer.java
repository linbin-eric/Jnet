package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends AbstractDirectBuffer implements PooledBuffer<ByteBuffer>
{
    protected Arena<ByteBuffer>         arena;
    protected ChunkListNode<ByteBuffer> chunkListNode;
    protected Chunk<ByteBuffer>         chunk;
    protected long                      handle;

    @Override
    public void init(Arena<ByteBuffer> arena, ChunkListNode<ByteBuffer> chunkListNode, Chunk<ByteBuffer> chunk, int capacity, int offset, long handle)
    {
        this.arena = arena;
        this.chunkListNode = chunkListNode;
        this.chunk = chunk;
        this.handle = handle;
        init(chunk.memory(), capacity, offset);
    }

    @Override
    public Chunk<ByteBuffer> chunk()
    {
        return chunk;
    }

    @Override
    public long handle()
    {
        return handle;
    }

    @Override
    protected long getAddress(ByteBuffer memory)
    {
        return chunk.directChunkAddress();
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        arena.reAllocate(chunkListNode, this, reqCapacity);
    }

    @Override
    public void free0(int capacity)
    {
        arena.free(chunkListNode, chunk, handle, capacity);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
