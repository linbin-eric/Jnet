package com.jfirer.jnet.common.buffer;

public interface PooledBuffer<T> extends IoBuffer
{
    void init(Chunk<T> chunk, int capacity, int offset, long handle, ThreadCache cache);

    void initUnPooled(Chunk<T> chunk, ThreadCache cache);

    PoolInfoHolder getPoolInfoHolder();

    Chunk<T> chunk();

    long handle();
}
