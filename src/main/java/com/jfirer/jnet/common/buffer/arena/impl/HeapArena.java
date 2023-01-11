package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Chunk;

public class HeapArena extends AbstractArena<byte[]>
{
    public HeapArena(int maxLevel, int pageSize, String name)
    {
        super(maxLevel, pageSize, name);
    }

    @Override
    protected ChunkListNode newChunk(int maxLevel, int pageSize, ChunkList chunkList)
    {
        return new HeapChunk(maxLevel, pageSize, chunkList);
    }

    @Override
    protected Chunk<byte[]> newHugeChunk(int reqCapacity)
    {
        return new HeapChunk(reqCapacity);
    }

    @Override
    protected void destoryChunk(Chunk<byte[]> chunk)
    {
    }

    @Override
    protected void memoryCopy(byte[] src, int srcOffset, byte[] desc, int destOffset, int oldWritePosi)
    {
        System.arraycopy(src, srcOffset, desc, destOffset, oldWritePosi);
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }
}
