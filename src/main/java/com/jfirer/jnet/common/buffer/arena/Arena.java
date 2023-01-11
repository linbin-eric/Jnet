package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.PooledBuffer;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;

public interface Arena<T>
{
    void allocate(int reqCapacity, PooledBuffer<T> buffer);

    void reAllocate(ChunkListNode<T> chunkListNode, PooledBuffer<T> buf, int newReqCapacity);

    void free(ChunkListNode<T> chunkListNode, Chunk<T> node, long handle, int capacity);
}
