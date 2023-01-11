package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;

public interface PooledBuffer<T> extends IoBuffer<T>
{
    void init(Arena<T> arena, Chunk<T> chunk, int capacity, int offset, long handle);

    Chunk<T> chunk();

    long handle();
}
