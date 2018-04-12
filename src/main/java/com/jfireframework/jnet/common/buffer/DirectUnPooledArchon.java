package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class DirectUnPooledArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, IoBuffer buffer)
    {
        buffer.initialize(0, need, ByteBuffer.allocateDirect(need), 0, null, this);
    }
    
    @Override
    public void expansion(IoBuffer buffer, int newSize)
    {
        IoBuffer expansionIoBuffer = IoBuffer.directBuffer();
        apply(newSize, expansionIoBuffer);
        buffer.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
    }
    
}
