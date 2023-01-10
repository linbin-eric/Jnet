package com.jfirer.jnet.common.buffer;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends AbstractDirectBuffer implements PooledBuffer<ByteBuffer>
{
    protected long              handle;
    //    protected ThreadCache cache;
    protected Chunk<ByteBuffer> chunk;

    @Override
    public void init(Chunk<ByteBuffer> chunk, int capacity, int offset, long handle)
    {
        this.chunk = chunk;
        this.handle = handle;
        init(chunk.memory(), capacity, offset);
    }
    //    public void initUnPooled(ChunkImpl<ByteBuffer> chunk, ThreadCache cache)
//    {
//        poolInfoHolder.init(-1, cache, chunk);
//        init(chunk.memory, chunk.chunkSize, 0, 0, 0);
//    }
//    @Override
//    public PoolInfoHolder getPoolInfoHolder()
//    {
//        return poolInfoHolder;
//    }

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
//    @Override
//    public void reAllocate(int reqCapacity)
//    {
//        PooledBuffer.super.reAllocate(reqCapacity);
//    }

    @Override
    protected long getAddress(ByteBuffer memory)
    {
        return chunk.directChunkAddress();
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        if (chunk.isUnPooled())
        {
            ((HugeChunk) chunk).arena().reAllocate(this, reqCapacity);
        }
        else
        {
            ((ChunkListNode) chunk).getParent().getArena().reAllocate(this, reqCapacity);
        }
    }

    @Override
    public void free0(int capacity)
    {
        if (chunk.isUnPooled())
        {
            ((HugeChunk) chunk).arena().free(chunk, handle, capacity);
        }
        else
        {
            ((ChunkListNode) chunk).getParent().getArena().free(chunk, handle, capacity);
        }
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
