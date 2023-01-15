package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;

public class PooledUnThreadCacheBufferAllocator extends PooledBufferAllocator
{
    public static final PooledUnThreadCacheBufferAllocator DEFAULT = new PooledUnThreadCacheBufferAllocator("PooledUnThreadCacheBufferAllocator_default");

    public PooledUnThreadCacheBufferAllocator(String name)
    {
        super(name);
    }
}
