package com.jfireframework.jnet.common.buffer;

public class PooledUnThreadCacheBufferAllocator extends PooledBufferAllocator
{
    public static final PooledUnThreadCacheBufferAllocator DEFAULT = new PooledUnThreadCacheBufferAllocator();
    
    public PooledUnThreadCacheBufferAllocator(int pagesize, int maxLevel, int numHeapArenas, int numDirectArenas, boolean preferDirect)
    {
        super(pagesize, maxLevel, numHeapArenas, numDirectArenas, 0, 0, 0, 0, false, preferDirect);
    }
    
    public PooledUnThreadCacheBufferAllocator()
    {
        this(PAGESIZE, MAXLEVEL, NUM_HEAP_ARENA, NUM_DIRECT_ARENA, true);
    }
}
