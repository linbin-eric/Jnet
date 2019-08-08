package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.buffer.Chunk;

public class CapacityStat
{
    private int freeBytes;
    private int chunkCapacity;
    private int numOfChunk;

    public void add(Chunk chunk)
    {
        freeBytes += chunk.getFreeBytes();
        chunkCapacity += chunk.getChunkSize();
    }

    public int getFreeBytes()
    {
        return freeBytes;
    }

    public int getChunkCapacity()
    {
        return chunkCapacity;
    }

    public int getNumOfChunk()
    {
        return numOfChunk;
    }
}
