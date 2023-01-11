package com.jfirer.jnet.common.buffer.arena.impl;

public class HeapChunk extends ChunkImpl<byte[]>
{
    public HeapChunk(int maxLevel, int pageSize)
    {
        super(maxLevel, pageSize);
    }

    public HeapChunk(int chunkSize)
    {
        super(chunkSize);
    }

    @Override
    protected byte[] initializeMemory(int size)
    {
        return new byte[size];
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }
}
