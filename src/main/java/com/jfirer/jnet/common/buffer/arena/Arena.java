package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.PooledBuffer;

public interface Arena<T>
{
    void allocate(int reqCapacity, PooledBuffer<T> buffer);

    void reAllocate(PooledBuffer<T> buf, int newReqCapacity);

    void free(Chunk<T> node, long handle, int capacity);
}
