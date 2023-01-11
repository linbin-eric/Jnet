package com.jfirer.jnet.common.buffer.arena.impl;

public class HeapChunk extends ChunkListNode<byte[]>
{
    public HeapChunk(int maxLevel, int pageSize, ChunkList<byte[]> parent)
    {
        super(maxLevel, pageSize, parent);
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
