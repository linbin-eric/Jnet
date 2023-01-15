package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;

public class PooledUnRecycleBufferAllocator extends PooledBufferAllocator
{
    public static final PooledUnRecycleBufferAllocator DEFAULT = new PooledUnRecycleBufferAllocator("PooledUnRecycleBufferAllocator_default");

    public PooledUnRecycleBufferAllocator(String name)
    {
        super(name);
    }
}
