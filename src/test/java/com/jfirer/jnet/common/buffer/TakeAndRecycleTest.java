package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TakeAndRecycleTest
{
    int                   pagesize  = 4096;
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator(pagesize, 4, 1, true, "testDirect");
    PooledBufferAllocator allocatorHeap = new PooledBufferAllocator(pagesize, 4, 1, false, "testHeap");

    @Test
    public void test()
    {
        test0(allocatorDirect);
        test0(allocatorHeap);
    }

    private void test0(PooledBufferAllocator allocator)
    {
        UnPooledBuffer buffer = (UnPooledBuffer) allocator.ioBuffer(pagesize);
        assertEquals(16, ((PooledStorageSegment) buffer.getStorageSegment()).getHandle());
        UnPooledBuffer buffer2 = (UnPooledBuffer) allocator.ioBuffer(pagesize << 1);
        assertEquals(9, ((PooledStorageSegment) buffer2.getStorageSegment()).getHandle());
        buffer.free();
        UnPooledBuffer buffer3 = (UnPooledBuffer) allocator.ioBuffer(pagesize << 1);
        assertEquals(8, ((PooledStorageSegment) buffer3.getStorageSegment()).getHandle());
        UnPooledBuffer buffer4 = (UnPooledBuffer) allocator.ioBuffer(pagesize);
        assertEquals(20, ((PooledStorageSegment) buffer4.getStorageSegment()).getHandle());
        UnPooledBuffer buffer5 = (UnPooledBuffer) allocator.ioBuffer(pagesize << 1);
        assertEquals(11, ((PooledStorageSegment) buffer5.getStorageSegment()).getHandle());
        UnPooledBuffer buffer6 = (UnPooledBuffer) allocator.ioBuffer(pagesize);
        assertEquals(21, ((PooledStorageSegment) buffer6.getStorageSegment()).getHandle());
        buffer2.free();
        UnPooledBuffer buffer7 = (UnPooledBuffer) allocator.ioBuffer(pagesize << 1);
        assertEquals(9, ((PooledStorageSegment) buffer7.getStorageSegment()).getHandle());
    }
}
