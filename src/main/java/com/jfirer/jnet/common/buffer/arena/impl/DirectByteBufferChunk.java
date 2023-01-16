package com.jfirer.jnet.common.buffer.arena.impl;

import java.nio.ByteBuffer;

public class DirectByteBufferChunk extends ChunkImpl<ByteBuffer>
{
    public DirectByteBufferChunk(int maxLevel, int pageSize)
    {
        super(maxLevel, pageSize);
    }

    public DirectByteBufferChunk(int size)
    {
        super(size);
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
