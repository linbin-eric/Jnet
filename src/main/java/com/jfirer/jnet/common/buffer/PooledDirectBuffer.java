package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends AbstractDirectBuffer implements PooledBuffer<ByteBuffer>
{

    final PoolInfoHolder poolInfoHolder = new PoolInfoHolder();

    public void init(ChunkImpl<ByteBuffer> chunk, int capacity, int offset, long handle, ThreadCache cache)
    {
        poolInfoHolder.init(handle, cache, chunk);
        init(chunk.memory, capacity, 0, 0, offset);
    }

    public void initUnPooled(ChunkImpl<ByteBuffer> chunk, ThreadCache cache)
    {
        poolInfoHolder.init(-1, cache, chunk);
        init(chunk.memory, chunk.chunkSize, 0, 0, 0);
    }

    @Override
    public PoolInfoHolder getPoolInfoHolder()
    {
        return poolInfoHolder;
    }

    @Override
    public ChunkImpl<ByteBuffer> chunk()
    {
        return poolInfoHolder.chunk;
    }

    @Override
    public long handle()
    {
        return poolInfoHolder.handle;
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        poolInfoHolder.chunk.arena.reAllocate(this, reqCapacity);
    }

    @Override
    protected void free0()
    {
        poolInfoHolder.free(capacity);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
