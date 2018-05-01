package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class UnPooledDirectArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, PooledIoBuffer buffer)
    {
        buffer.initialize(0, need, ByteBuffer.allocateDirect(need), 0, null, this);
    }
    
    @Override
    public void expansion(PooledIoBuffer buffer, int newSize)
    {
        PooledIoBuffer expansionIoBuffer = PooledIoBuffer.directBuffer();
        apply(newSize, expansionIoBuffer);
        buffer.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
    }
    
}
