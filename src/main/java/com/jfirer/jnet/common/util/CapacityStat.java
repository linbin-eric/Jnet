package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.buffer.arena.Chunk;
import lombok.Data;
import lombok.ToString;

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
        freeBytes          = 0;
        chunkCapacity      = 0;
        numOfPooledChunk   = 0;
        numOfUnPooledChunk = 0;
        usedAllocate       = 0;
    }

    @Override
    public String toString()
    {
        return "CapacityStat{usedBytes=" + (chunkCapacity - freeBytes) / 1024 +
               "K,freeBytes=" + freeBytes / 1024 +
               "K, chunkCapacity=" + chunkCapacity / 1024 +
               "K, numOfPooledChunk=" + numOfPooledChunk +
               ", numOfUnPooledChunk=" + numOfUnPooledChunk +
               ", usedAllocate=" + usedAllocate +
               '}';
    }
}
