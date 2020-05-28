package com.jfirer.jnet.common.buffer;

public class PooledUnRecycleBufferAllocator extends PooledBufferAllocator
{
    public static final PooledUnRecycleBufferAllocator DEFAULT = new PooledUnRecycleBufferAllocator("PooledUnRecycleBufferAllocator_default");

    public PooledUnRecycleBufferAllocator(int pagesize, int maxLevel, int numHeapArenas, int numDirectArenas, boolean preferDirect, String name)
    {
        super(pagesize, maxLevel, numHeapArenas, numDirectArenas, 0, 0, 0, 0, false, preferDirect, name);
    }

    public PooledUnRecycleBufferAllocator(String name)
    {
        this(PAGESIZE, MAXLEVEL, NUM_HEAP_ARENA, NUM_DIRECT_ARENA, true, name);
    }

    @Override
    public IoBuffer heapBuffer(int initializeCapacity)
    {
        ThreadCache      threadCache = localCache.get();
        HeapArena        heapArena   = threadCache.heapArena;
        PooledHeapBuffer buffer      = new PooledHeapBuffer();
        heapArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
        return buffer;
    }

    @Override
    public IoBuffer directBuffer(int initializeCapacity)
    {
        ThreadCache        threadCache = localCache.get();
        DirectArena        directArena = threadCache.directArena;
        PooledDirectBuffer buffer      = new PooledDirectBuffer();
        directArena.allocate(initializeCapacity, Integer.MAX_VALUE, buffer, threadCache);
        return buffer;
    }
}
