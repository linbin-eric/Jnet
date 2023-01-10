package com.jfirer.jnet.common.buffer;

public interface PooledBuffer<T> extends IoBuffer
{
//    void init(ChunkImpl<T> chunk, int capacity, int offset, long handle, ThreadCache cache);

    void init(Chunk<T> node, int capacity, int offset, long handle);
//    void init(HugeChunk<T> hugeChunk);
//    void initUnPooled(ChunkImpl<T> chunk, ThreadCache cache);

    Chunk<T> chunk();

    long handle();
}
