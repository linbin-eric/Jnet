package com.jfirer.jnet.common.buffer;

public class PoolInfoHolder
{
    protected long        handle;
    protected ThreadCache cache;
    protected Chunk       chunk;

    public void init(long handle, ThreadCache cache, Chunk chunk)
    {
        this.handle = handle;
        this.cache = cache;
        this.chunk = chunk;
    }

    public void free(int capacity)
    {
        chunk.arena.free(chunk, handle, capacity, cache);
        handle = 0;
        cache = null;
        chunk = null;
    }
}
