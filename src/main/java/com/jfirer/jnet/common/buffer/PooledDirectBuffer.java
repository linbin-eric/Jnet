package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends AbstractDirectBuffer implements PooledBuffer<ByteBuffer>
{
    protected long              handle;
    protected Chunk<ByteBuffer> chunk;
    protected Arena<ByteBuffer> arena;

    @Override
    public void init(Arena<ByteBuffer> arena, Chunk<ByteBuffer> chunk, int capacity, int offset, long handle)
    {
        this.arena = arena;
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
        arena.reAllocate(this, reqCapacity);
    }

    @Override
    public void free0(int capacity)
    {
        arena.free(chunk, handle, capacity);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
