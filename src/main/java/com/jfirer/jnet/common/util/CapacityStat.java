package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.buffer.arena.Chunk;
import lombok.Data;

@Data
public class CapacityStat
{
    private int freeBytes;
    private int chunkCapacity;
    private int numOfPooledChunk;
    private int numOfUnPooledChunk;
    private int usedAllocate;

    public void add(Chunk chunk)
    {
        freeBytes += chunk.getFreeBytes();
        chunkCapacity += chunk.getChunkSize();
        numOfPooledChunk += 1;
    }

    public void clear()
    {
        freeBytes = 0;
        chunkCapacity = 0;
        numOfPooledChunk = 0;
        numOfUnPooledChunk = 0;
    }
}
