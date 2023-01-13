package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.arena.impl.HeapArena;
import com.jfirer.jnet.common.buffer.buffer.PooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

public class PooledHeapBuffer extends AbstractHeapBuffer implements PooledBuffer<byte[]>
{
    protected long                             handle;
    protected HeapArena                        arena;
    protected ChunkListNode<byte[]>            chunkListNode;
    protected RecycleHandler<PooledHeapBuffer> recycleHandler;
    static    Recycler<PooledHeapBuffer>       RECYCLER = new Recycler<>(function -> {
        PooledHeapBuffer                 buffer  = new PooledHeapBuffer();
        RecycleHandler<PooledHeapBuffer> handler = function.apply(buffer);
        buffer.recycleHandler = handler;
        return buffer;
    });

    public static PooledHeapBuffer allocate(HeapArena arena, int reqCapacity)
    {
        PooledHeapBuffer pooledHeapBuffer = RECYCLER.get();
        arena.allocate(reqCapacity, pooledHeapBuffer);
        return pooledHeapBuffer;
    }

    @Override
    public void init(Arena<byte[]> arena, ChunkListNode<byte[]> chunkListNode, int capacity, int offset, long handle)
    {
        this.arena = (HeapArena) arena;
        this.chunkListNode = chunkListNode;
        this.handle = handle;
        init(chunkListNode.memory(), capacity, offset);
    }

    @Override
    public Chunk<byte[]> chunk()
    {
        return chunkListNode;
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
        arena.free(chunkListNode, handle, capacity);
        recycleHandler.recycle(this);
    }
}
