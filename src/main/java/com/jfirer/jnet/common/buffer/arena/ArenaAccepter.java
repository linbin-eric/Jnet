package com.jfirer.jnet.common.buffer.arena;

public interface ArenaAccepter
{
    void init(Arena arena, Chunk chunk, long handle, int offset, int capacity);
}
