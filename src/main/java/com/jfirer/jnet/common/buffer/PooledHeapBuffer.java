package com.jfirer.jnet.common.buffer;

public class PooledHeapBuffer extends AbstractHeapBuffer implements PooledBuffer<byte[]>
{
    protected long          handle;
    protected Chunk<byte[]> chunk;

    @Override
    public void init(Chunk<byte[]> chunk, int capacity, int offset, long handle)
    {
        init(chunk.memory(), capacity, offset);
        this.handle = handle;
        this.chunk = chunk;
    }

    @Override
    public Chunk<byte[]> chunk()
    {
        return chunk;
    }

    @Override
    public long handle()
    {
        return handle;
    }

    @Override
    protected long getAddress(byte[] memory)
    {
        return 0;
    }

    @Override
    public void reAllocate(int reqCapacity)
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
        return SliceHeapBuffer.slice(this, length);
    }
}
