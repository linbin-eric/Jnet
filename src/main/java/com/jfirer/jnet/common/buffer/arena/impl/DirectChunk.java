package com.jfirer.jnet.common.buffer.arena.impl;

import java.nio.ByteBuffer;

public class DirectChunk extends ChunkListNode<ByteBuffer>
{
    public DirectChunk(int maxLevel, int pageSize, ChunkList<ByteBuffer> parent)
    {
        super(maxLevel, pageSize, parent);
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
