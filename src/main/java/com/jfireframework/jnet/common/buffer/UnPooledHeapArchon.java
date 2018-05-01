package com.jfireframework.jnet.common.buffer;

class UnPooledHeapArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, PooledIoBuffer handler)
    {
        handler.initialize(0, need, new byte[need], 0, null, this);
    }
    
    @Override
    public void expansion(PooledIoBuffer handler, int newSize)
    {
        PooledIoBuffer expansionIoBuffer = PooledIoBuffer.heapIoBuffer();
        apply(newSize, expansionIoBuffer);
        handler.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
        
    }
}
