package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

public class PoolInfoHolder
{
    protected long        handle;
    protected ThreadCache cache;
    protected ChunkImpl   chunk;

    public void init(long handle, ThreadCache cache, ChunkImpl chunk)
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
