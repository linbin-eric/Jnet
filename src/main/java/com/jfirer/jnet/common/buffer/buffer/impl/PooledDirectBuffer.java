package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.arena.impl.DirectArena;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.PooledBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends AbstractDirectBuffer implements PooledBuffer<ByteBuffer>
{
    protected DirectArena                        arena;
    protected ChunkListNode<ByteBuffer>          chunkListNode;
    protected Chunk<ByteBuffer>                  chunk;
    protected long                               handle;
    protected RecycleHandler<PooledDirectBuffer> recycleHandler;
    static    Recycler<PooledDirectBuffer>       RECYCLER = new Recycler<>(function -> {
        PooledDirectBuffer                 buffer  = new PooledDirectBuffer();
        RecycleHandler<PooledDirectBuffer> handler = function.apply(buffer);
        buffer.recycleHandler = handler;
        return buffer;
    });

    public static PooledDirectBuffer allocate(DirectArena arena, int reqCapacity)
    {
        PooledDirectBuffer pooledDirectBuffer = RECYCLER.get();
        arena.allocate(reqCapacity, pooledDirectBuffer);
        return pooledDirectBuffer;
    }

    @Override
    public void init(Arena<ByteBuffer> arena, ChunkListNode<ByteBuffer> chunkListNode, Chunk<ByteBuffer> chunk, int capacity, int offset, long handle)
    {
        this.arena = (DirectArena) arena;
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
        recycleHandler.recycle(this);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return SliceDirectBuffer.slice(this, length);
    }
}
