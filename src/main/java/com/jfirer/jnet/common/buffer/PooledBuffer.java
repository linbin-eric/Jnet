package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

public interface PooledBuffer<T> extends IoBuffer
{
    void init(ChunkImpl<T> chunk, int capacity, int offset, long handle, ThreadCache cache);

    void init(ChunkListNode node, int capacity, int offset, long handle);

    void initUnPooled(ChunkImpl<T> chunk, ThreadCache cache);

    PoolInfoHolder getPoolInfoHolder();

    ChunkImpl<T> chunk();

    long handle();
}
