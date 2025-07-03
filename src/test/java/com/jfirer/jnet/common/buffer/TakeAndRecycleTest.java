package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TakeAndRecycleTest
{
    int                   pagesize        = PooledBufferAllocator.PAGESIZE;
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator(100, true, new Arena("direct", BufferType.UNSAFE));
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator(100, false, new Arena("heap", BufferType.HEAP));

    @Test
    public void test()
    {
        test0(allocatorDirect);
        test0(allocatorHeap);
    }

    private void test0(PooledBufferAllocator allocator)
    {
        PooledBuffer buffer = (PooledBuffer) allocator.allocate(pagesize);
        assertEquals(2048, (buffer).getHandle());
        PooledBuffer buffer2 = (PooledBuffer) allocator.allocate(pagesize << 1);
        assertEquals(1025, buffer2.getHandle());
        buffer.free();
        PooledBuffer buffer3 = (PooledBuffer) allocator.allocate(pagesize << 1);
        assertEquals(1024, buffer3.getHandle());
        PooledBuffer buffer4 = (PooledBuffer) allocator.allocate(pagesize);
        assertEquals(2052, buffer4.getHandle());
        PooledBuffer buffer5 = (PooledBuffer) allocator.allocate(pagesize << 1);
        assertEquals(1027, buffer5.getHandle());
        PooledBuffer buffer6 = (PooledBuffer) allocator.allocate(pagesize);
        assertEquals(2053, (buffer6).getHandle());
        buffer2.free();
        PooledBuffer buffer7 = (PooledBuffer) allocator.allocate(pagesize << 1);
        assertEquals(1025, buffer7.getHandle());
    }
}
