package com.jfirer.jnet.common.buffer.arena;

public interface ArenaAccepter
{
    void init(Arena arena, ChunkListNode chunkListNode, long handle, int offset, int capacity);
}
