package com.jfirer.jnet.common.buffer;

public interface HugeChunk<T> extends Chunk<T>
{
    Arena<T> arena();
}
