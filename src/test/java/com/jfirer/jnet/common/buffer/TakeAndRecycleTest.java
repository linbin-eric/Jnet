package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator2;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer2;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TakeAndRecycleTest
{
    int                    pagesize        = PooledBufferAllocator2.PAGESIZE;
    PooledBufferAllocator2 allocatorDirect = new PooledBufferAllocator2(100, true, new Arena("direct", BufferType.UNSAFE));
    PooledBufferAllocator2 allocatorHeap   = new PooledBufferAllocator2(100, false, new Arena("heap", BufferType.HEAP));

    @Test
    public void test()
    {
        test0(allocatorDirect);
        test0(allocatorHeap);
    }

    private void test0(PooledBufferAllocator2 allocator)
    {
        PooledBuffer2 buffer = (PooledBuffer2) allocator.ioBuffer(pagesize);
        assertEquals(2048, (buffer).getHandle());
        PooledBuffer2 buffer2 = (PooledBuffer2) allocator.ioBuffer(pagesize << 1);
        assertEquals(1025, buffer2.getHandle());
        buffer.free();
        PooledBuffer2 buffer3 = (PooledBuffer2) allocator.ioBuffer(pagesize << 1);
        assertEquals(1024, buffer3.getHandle());
        PooledBuffer2 buffer4 = (PooledBuffer2) allocator.ioBuffer(pagesize);
        assertEquals(2052, buffer4.getHandle());
        PooledBuffer2 buffer5 = (PooledBuffer2) allocator.ioBuffer(pagesize << 1);
        assertEquals(1027, buffer5.getHandle());
        PooledBuffer2 buffer6 = (PooledBuffer2) allocator.ioBuffer(pagesize);
        assertEquals(2053, (buffer6).getHandle());
        buffer2.free();
        PooledBuffer2 buffer7 = (PooledBuffer2) allocator.ioBuffer(pagesize << 1);
        assertEquals(1025, buffer7.getHandle());
    }
}
