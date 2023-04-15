package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.buffer.arena.Chunk;

public class CapacityStat
{
    private int freeBytes;
    private int chunkCapacity;
    private int numOfPooledChunk;
    private int numOfUnPooledChunk;

    public void add(Chunk chunk)
    {
        freeBytes += chunk.getFreeBytes();
        chunkCapacity += chunk.getChunkSize();
        numOfPooledChunk += 1;
    }

    public int getFreeBytes()
    {
        return freeBytes;
    }

    public int getChunkCapacity()
    {
        return chunkCapacity;
    }

    public int getNumOfPooledChunk()
    {
        return numOfPooledChunk;
    }

    public int getNumOfUnPooledChunk()
    {
        return numOfUnPooledChunk;
    }

    public void clear()
    {
        freeBytes = 0;
        chunkCapacity = 0;
        numOfPooledChunk = 0;
        numOfUnPooledChunk = 0;
    }

    public void setNumOfUnPooledChunk(int numOfUnPooledChunk)
    {
        this.numOfUnPooledChunk = numOfUnPooledChunk;
    }

    @Override
    public String toString()
    {
        return "CapacityStat{" + "freeBytes=" + freeBytes + ", chunkCapacity=" + chunkCapacity + ", numOfPooledChunk=" + numOfPooledChunk + ", numOfUnPooledChunk=" + numOfUnPooledChunk + '}';
    }
}
