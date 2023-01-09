package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

public class HeapArena extends AbstractArena<byte[]>
{
    public HeapArena(PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask, String name)
    {
        super(parent, maxLevel, pageSize, pageSizeShift, subpageOverflowMask, name);
    }

    @Override
    void destoryChunk(ChunkImpl<byte[]> chunk)
    {
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }

    @Override
    ChunkImpl<byte[]> newChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        return new HeapChunk(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
    }

    @Override
    ChunkImpl<byte[]> newChunk(int reqCapacity, AbstractArena<byte[]> tAbstractArena)
    {
        return new HeapChunk(reqCapacity);
    }

    @Override
    void memoryCopy(byte[] src, int srcOffset, byte[] desc, int destOffset, int oldWritePosi)
    {
        System.arraycopy(src, srcOffset, desc, destOffset, oldWritePosi);
    }
}
