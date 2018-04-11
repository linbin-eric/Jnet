package com.jfireframework.jnet.common.buffer;

public abstract class UnPooledArchon extends Archon
{
    
    @Override
    public void recycle(IoBuffer handler)
    {
        
    }
    
    public static UnPooledArchon directUnPooledArchon()
    {
        return new DirectUnPooledArchon();
    }
    
    public static UnPooledArchon heapUnPooledArchon()
    {
        return new HeapUnPooledArchon();
    }
}
