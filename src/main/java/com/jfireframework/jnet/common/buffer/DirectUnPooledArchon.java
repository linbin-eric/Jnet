package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class DirectUnPooledArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, AbstractIoBuffer buffer)
    {
        buffer.initialize(0, need, ByteBuffer.allocateDirect(need), 0, null, this);
    }
    
    @Override
    public void expansion(AbstractIoBuffer buffer, int newSize)
    {
        AbstractIoBuffer expansionIoBuffer = AbstractIoBuffer.directBuffer();
        apply(newSize, expansionIoBuffer);
        buffer.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
    }
    
}
