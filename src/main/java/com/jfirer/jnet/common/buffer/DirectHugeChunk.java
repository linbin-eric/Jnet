package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

import java.nio.ByteBuffer;

public class DirectHugeChunk extends ChunkImpl<ByteBuffer> implements HugeChunk<ByteBuffer>
{
    private Arena<ByteBuffer> arena;

    public DirectHugeChunk(int chunkSize, Arena<ByteBuffer> arena)
    {
        super(chunkSize);
        this.arena = arena;
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

    @Override
    public Arena<ByteBuffer> arena()
    {
        return arena;
    }
}
