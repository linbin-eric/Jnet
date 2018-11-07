package com.jfireframework.jnet.common.buffer;

public class HeapChunk extends Chunk<byte[]>
{

    public HeapChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        super(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
    }

    public HeapChunk(int chunkSize)
    {
        super(chunkSize);
    }

    @Override
    byte[] initializeMemory(int size)
    {
        return new byte[size];
    }

    @Override
    public boolean isDirect()
    {
        return false;
    }
}
