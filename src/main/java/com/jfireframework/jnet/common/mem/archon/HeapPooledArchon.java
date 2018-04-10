package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class HeapPooledArchon extends PooledArchon<byte[]>
{
    
    public HeapPooledArchon(int maxLevel, int unit)
    {
        super(maxLevel, unit);
        expansionIoBuffer = new HeapIoBufferHandler();
    }
    
    @Override
    protected void initHugeBucket(IoBuffer<byte[]> handler, int need)
    {
        handler.initialize(0, need, new byte[need], 0, null, null);
    }
    
    @Override
    protected Chunk<byte[]> newChunk(int maxLevel, int unit)
    {
        HeapChunk heapChunk = new HeapChunk(maxLevel, unit);
        return heapChunk;
    }
    
    class HeapIoBufferHandler extends HeapIoBuffer implements ExpansionIoBuffer<byte[]>
    {
        
        @Override
        public void clearForNextCall()
        {
            chunk = null;
            mem = null;
            index = -1;
            capacity = -1;
        }
        
    }
}
