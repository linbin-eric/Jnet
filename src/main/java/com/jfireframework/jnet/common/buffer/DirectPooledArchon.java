package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class DirectPooledArchon extends PooledArchon
{
    
    DirectPooledArchon(int maxLevel, int unit)
    {
        super(maxLevel, unit);
        expansionIoBuffer = AbstractIoBuffer.directBuffer();
    }
    
    @Override
    protected void initHugeBucket(AbstractIoBuffer handler, int need)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(need);
        handler.initialize(0, need, byteBuffer, 0, null, null);
    }
    
    @Override
    protected Chunk newChunk(int maxLevel, int unit)
    {
        return Chunk.newDirectChunk(maxLevel, unit);
    }
    
}
