package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;

public interface Arena<T>
{
    void allocate(int reqCapacity, PoolableBuffer<T> buffer);

    void reAllocate(ChunkListNode<T> chunkListNode, PoolableBuffer<T> buf, int newReqCapacity);

    void free(ChunkListNode<T> chunkListNode, long handle, int capacity);
}
