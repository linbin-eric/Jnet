package com.jfireframework.jnet.common.buffer;

public class ChunkStoreList extends ChunkList
{
    private int chunkSum         = 0;
    private int chunkSumtTreshol = 100;
    
    public ChunkStoreList(ChunkList next, int maxUsage, int minUsage, String name)
    {
        super(next, maxUsage, minUsage, name);
        this.name = "ChunkStoreList";
    }
    
    @Override
    public void addChunk(Chunk chunk)
    {
        if (chunkSum >= chunkSumtTreshol)
        {
            return;
        }
        super.addChunk(chunk);
        if (chunk.parent == this)
        {
            chunkSum += 1;
        }
    }
    
    @Override
    protected void removeFromCurrentList(Chunk node)
    {
        super.removeFromCurrentList(node);
        chunkSum -= 1;
    }
}
