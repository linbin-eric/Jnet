package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.PooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

public class PooledHeapBuffer extends AbstractHeapBuffer implements PooledBuffer<byte[]>
{
    protected long                             handle;
    protected Chunk<byte[]>                    chunk;
    protected Arena<byte[]>                    arena;
    protected ChunkListNode<byte[]>            chunkListNode;
    protected RecycleHandler<PooledHeapBuffer> recycleHandler;
    static    Recycler<PooledHeapBuffer>       RECYCLER = new Recycler<>(function -> {
        PooledHeapBuffer                 buffer  = new PooledHeapBuffer();
        RecycleHandler<PooledHeapBuffer> handler = function.apply(buffer);
        buffer.recycleHandler = handler;
        return buffer;
    });

    public static PooledHeapBuffer allocate(Arena<byte[]> arena, int reqCapacity)
    {
        PooledHeapBuffer pooledHeapBuffer = RECYCLER.get();
        arena.allocate(reqCapacity, pooledHeapBuffer);
        return pooledHeapBuffer;
    }

    @Override
    public void init(Arena<byte[]> arena, ChunkListNode<byte[]> chunkListNode, Chunk<byte[]> chunk, int capacity, int offset, long handle)
    {
        this.arena = arena;
        this.chunkListNode = chunkListNode;
        this.chunk = chunk;
        this.handle = handle;
        init(chunk.memory(), capacity, offset);
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
        arena.reAllocate(chunkListNode, this, reqCapacity);
    }

    @Override
    public void free0(int capacity)
    {
        arena.free(chunkListNode, chunk, handle, capacity);
        recycleHandler.recycle(this);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceHeapBuffer.slice(this, length);
    }
}
