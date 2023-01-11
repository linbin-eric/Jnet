package com.jfirer.jnet.common.buffer.arena.impl;

import java.nio.ByteBuffer;

public class DirectChunk extends ChunkImpl<ByteBuffer>
{
    public DirectChunk(int maxLevel, int pageSize)
    {
        super(maxLevel, pageSize);
    }

    public DirectChunk(int chunkSize)
    {
        super(chunkSize);
    }

    @Override
    protected ByteBuffer initializeMemory(int size)
    {
        return ByteBuffer.allocateDirect(size);
    }

    @Override
    public boolean isDirect()
    {
        return true;
    }
}
