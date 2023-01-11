package com.jfirer.jnet.common.buffer.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;

public interface PooledBuffer<T> extends IoBuffer<T>
{
    void init(Arena<T> arena, ChunkListNode<T> chunkListNode, Chunk<T> chunk, int capacity, int offset, long handle);

    Chunk<T> chunk();

    long handle();
}
