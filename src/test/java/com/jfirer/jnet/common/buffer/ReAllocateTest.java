package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ReAllocateTest
{
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator("test", false);
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator("test", true);

    @Test
    public void test()
    {
        test0(allocatorHeap);
        test0(allocatorDirect);
    }

    private void test0(PooledBufferAllocator allocator)
    {
        BasicBuffer buffer = (BasicBuffer) allocator.ioBuffer(16);
        int         offset = buffer.offset();
        long        handle = ((PooledStorageSegment) buffer.getStorageSegment()).getHandle();
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
        assertNotEquals(handle, ((PooledStorageSegment) buffer.getStorageSegment()).getHandle());
        assertEquals(4, buffer.getInt());
        assertEquals(5, buffer.getInt());
        assertEquals(6, buffer.getInt());
        assertEquals(7, buffer.getInt());
        assertEquals(8, buffer.getInt());
        assertEquals(0, buffer.remainRead());
    }
}
