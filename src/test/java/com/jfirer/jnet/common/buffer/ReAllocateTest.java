package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ReAllocateTest
{
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator(100, false, new Arena("1", BufferType.HEAP));
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator(100, true, new Arena("2", BufferType.UNSAFE));

    @Test
    public void test()
    {
        test0(allocatorHeap);
        test0(allocatorDirect);
    }

    private void test0(PooledBufferAllocator allocator)
    {
        PooledBuffer buffer = (PooledBuffer) allocator.allocate(16);
        int          offset = buffer.offset();
        long           handle = (buffer).getHandle();
        assertEquals(16, buffer.capacity());
        buffer.putInt(4);
        buffer.putInt(5);
        buffer.putInt(6);
        buffer.putInt(7);
        assertEquals(16, buffer.capacity());
        buffer.putInt(8);
        assertEquals(32, buffer.capacity());
        assertEquals(20, buffer.getWritePosi());
        assertEquals(0, buffer.getReadPosi());
        assertNotEquals(offset, buffer.offset());
        assertNotEquals(handle, (buffer).getHandle());
        assertEquals(4, buffer.getInt());
        assertEquals(5, buffer.getInt());
        assertEquals(6, buffer.getInt());
        assertEquals(7, buffer.getInt());
        assertEquals(8, buffer.getInt());
        assertEquals(0, buffer.remainRead());
    }
}
