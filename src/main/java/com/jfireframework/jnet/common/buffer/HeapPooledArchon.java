package com.jfireframework.jnet.common.buffer;

class HeapPooledArchon extends PooledArchon
{
    
    HeapPooledArchon(int maxLevel, int unit)
    {
        super(maxLevel, unit);
        expansionIoBuffer = AbstractIoBuffer.heapIoBuffer();
    }
    
    @Override
    protected void initHugeBucket(AbstractIoBuffer handler, int need)
    {
        handler.initialize(0, need, new byte[need], 0, null, null);
    }
    
    @Override
    protected Chunk newChunk(int maxLevel, int unit)
    {
        return Chunk.newHeapChunk(maxLevel, unit);
    }
}
