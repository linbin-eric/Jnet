package com.jfireframework.jnet.common.buffer;

class UnPooledHeapArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, AbstractIoBuffer handler)
    {
        handler.initialize(0, need, new byte[need], 0, null, this);
    }
    
    @Override
    public void expansion(AbstractIoBuffer handler, int newSize)
    {
        AbstractIoBuffer expansionIoBuffer = AbstractIoBuffer.heapIoBuffer();
        apply(newSize, expansionIoBuffer);
        handler.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
        
    }
}
