package com.jfirer.jnet.common.buffer;

public interface Arena
{
    void allocate(int reqCapacity, IoBuffer buffer);

    void free(ChunkListNode node, long handle, int capacity);
}
