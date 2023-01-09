package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

import java.nio.ByteBuffer;

public class DirectChunk extends ChunkImpl<ByteBuffer>
{
    public DirectChunk(int chunkSize)
    {
        super(chunkSize);
    }

    public DirectChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        super(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
    }

    @Override
    ByteBuffer initializeMemory(int size)
    {
        return ByteBuffer.allocateDirect(size);
    }

    @Override
    public boolean isDirect()
    {
        return true;
    }
}
