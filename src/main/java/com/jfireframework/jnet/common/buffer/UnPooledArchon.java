package com.jfireframework.jnet.common.buffer;

public abstract class UnPooledArchon extends Archon
{
    
    @Override
    public void recycle(IoBuffer handler)
    {
        
    }
    
    @Override
    public void recycle(IoBuffer[] buffers, int off, int len)
    {
        
    }
    
    public static UnPooledArchon directUnPooledArchon()
    {
        return new UnPooledDirectArchon();
    }
    
    public static UnPooledArchon heapUnPooledArchon()
    {
        return new UnPooledHeapArchon();
    }
}
