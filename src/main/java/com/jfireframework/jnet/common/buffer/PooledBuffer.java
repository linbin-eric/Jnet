package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.RecycleHandler;

public interface PooledBuffer<T> extends IoBuffer
{
    void init(Chunk<T> chunk, int capacity, int offset, long handle, ThreadCache cache);

    void initUnPooled(Chunk<T> chunk, ThreadCache cache);

    PoolInfoHolder getPoolInfoHolder();

    Chunk<T> chunk();

    long handle();
}
