package com.jfirer.jnet.common.buffer;

public class HeapArena extends AbstractArena<byte[]>
{
    public HeapArena(int maxLevel, int pageSize, String name)
    {
        super(maxLevel, pageSize, name);
    }

    @Override
    ChunkListNode newChunk(int maxLevel, int pageSize, ChunkList chunkList)
    {
        return new HeapChunk(maxLevel, pageSize, chunkList);
    }

    @Override
    HugeChunk<byte[]> newHugeChunk(int reqCapacity)
    {
        return new HeapHugeChunk(reqCapacity, this);
    }

    @Override
    void destoryChunk(Chunk<byte[]> chunk)
    {
    }

    @Override
    void memoryCopy(byte[] src, int srcOffset, byte[] desc, int destOffset, int oldWritePosi)
    {
        System.arraycopy(src, srcOffset, desc, destOffset, oldWritePosi);
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }
}
