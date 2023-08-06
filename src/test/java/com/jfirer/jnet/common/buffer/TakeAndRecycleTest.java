package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TakeAndRecycleTest
{
    int                   pagesize  = 4096;
    PooledBufferAllocator allocator = new PooledBufferAllocator(pagesize, 4, 1, true, "test");

    @Test
    public void test()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean preferDirect)
    {
        BasicBuffer buffer = (BasicBuffer) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(16, ((PooledStorageSegment) buffer.getStorageSegment()).getHandle());
        BasicBuffer buffer2 = (BasicBuffer) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(9, ((PooledStorageSegment) buffer2.getStorageSegment()).getHandle());
        ((BasicBuffer) buffer).free();
        BasicBuffer buffer3 = (BasicBuffer) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(8, ((PooledStorageSegment) buffer3.getStorageSegment()).getHandle());
        BasicBuffer buffer4 = (BasicBuffer) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(20, ((PooledStorageSegment) buffer4.getStorageSegment()).getHandle());
        BasicBuffer buffer5 = (BasicBuffer) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(11, ((PooledStorageSegment) buffer5.getStorageSegment()).getHandle());
        BasicBuffer buffer6 = (BasicBuffer) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(21, ((PooledStorageSegment) buffer6.getStorageSegment()).getHandle());
        ((BasicBuffer) buffer2).free();
        BasicBuffer buffer7 = (BasicBuffer) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(9, ((PooledStorageSegment) buffer7.getStorageSegment()).getHandle());
    }
}
