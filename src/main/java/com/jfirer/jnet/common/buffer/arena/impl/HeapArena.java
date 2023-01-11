package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Chunk;

public class HeapArena extends AbstractArena<byte[]>
{
    public HeapArena(int maxLevel, int pageSize, String name)
    {
        super(maxLevel, pageSize, name);
    }

    @Override
    protected HeapChunk newChunk(int maxLevel, int pageSize)
    {
        return new HeapChunk(maxLevel, pageSize);
    }

    @Override
    protected HeapChunk newHugeChunk(int reqCapacity)
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
