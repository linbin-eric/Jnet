package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

public class HeapHugeChunk extends ChunkImpl<byte[]> implements HugeChunk<byte[]>
{
    private Arena<byte[]> arena;

    public HeapHugeChunk(int chunkSize, Arena<byte[]> arena)
    {
        super(chunkSize);
        this.arena = arena;
    }

    @Override
    public Arena<byte[]> arena()
    {
        return arena;
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
