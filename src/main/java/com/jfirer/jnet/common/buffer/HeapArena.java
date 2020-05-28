package com.jfirer.jnet.common.buffer;

public class HeapArena extends Arena<byte[]>
{

    public HeapArena(PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask, String name)
    {
        super(parent, maxLevel, pageSize, pageSizeShift, subpageOverflowMask, name);
    }

    @Override
    void destoryChunk(Chunk<byte[]> chunk)
    {
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }

    @Override
    Chunk<byte[]> newChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        return new HeapChunk(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
    }

    @Override
    Chunk<byte[]> newChunk(int reqCapacity)
    {
        return new HeapChunk(reqCapacity);
    }

    @Override
    void memoryCopy(byte[] src, int srcOffset, byte[] desc, int destOffset, int oldWritePosi)
    {
        System.arraycopy(src, srcOffset, desc, destOffset, oldWritePosi);
    }
}
