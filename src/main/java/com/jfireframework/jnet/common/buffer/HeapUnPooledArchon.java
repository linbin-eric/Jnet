package com.jfireframework.jnet.common.buffer;

class HeapUnPooledArchon extends UnPooledArchon
{
    
    @Override
    public void apply(int need, IoBuffer handler)
    {
        handler.initialize(0, need, new byte[need], 0, null, this);
    }
    
    @Override
    public void expansion(IoBuffer handler, int newSize)
    {
        IoBuffer expansionIoBuffer = IoBuffer.heapIoBuffer();
        apply(newSize, expansionIoBuffer);
        handler.expansion(expansionIoBuffer);
        recycle(expansionIoBuffer);
        expansionIoBuffer.destory();
        
    }
}
