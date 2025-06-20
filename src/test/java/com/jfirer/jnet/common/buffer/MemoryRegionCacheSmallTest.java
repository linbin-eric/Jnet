package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.CachedBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.CachedStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.LinkedList;
import java.util.List;

@RunWith(Parameterized.class)
public class MemoryRegionCacheSmallTest
{
    CachedBufferAllocator allocatorDirect = new CachedBufferAllocator("testDirect", true);
    CachedBufferAllocator allocatorHeap   = new CachedBufferAllocator("testHeap", false);
    private final int size;

    public MemoryRegionCacheSmallTest(int size)
    {
        this.size = size;
    }

    @Parameterized.Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           i    = 16;
        while (i < CachedBufferAllocator.PAGESIZE)
        {
            list.add(i);
            i <<= 1;
        }
        return list;
    }

    @Test
    public void test() throws InterruptedException
    {
        test0(allocatorHeap, size);
        test0(allocatorDirect, size);
    }

    @SuppressWarnings("unchecked")
    private void test0(CachedBufferAllocator allocator, int size) throws InterruptedException
    {
        int                     numOfCached = CachedBufferAllocator.NUM_OF_CACHE;
        final List<BasicBuffer> buffers     = new LinkedList<>();
        for (int i = 0; i < numOfCached; i++)
        {
            BasicBuffer buffer = (BasicBuffer) allocator.ioBuffer(size);
            buffers.add(buffer);
            CachedStorageSegment storageSegment = (CachedStorageSegment) buffer.getStorageSegment();
            Assert.assertNotNull(storageSegment.getThreadCache());
            Assert.assertEquals(i, storageSegment.getBitMapIndex());
        }
        BasicBuffer ioBuffer = (BasicBuffer) allocator.ioBuffer(size);
        Assert.assertTrue(ioBuffer.getStorageSegment() instanceof PooledStorageSegment);
        Assert.assertNotNull(((PooledStorageSegment) ioBuffer.getStorageSegment()).getArena());
        ioBuffer.free();
        ioBuffer = (BasicBuffer) allocator.ioBuffer(size);
        Assert.assertTrue(ioBuffer.getStorageSegment() instanceof PooledStorageSegment);
        ioBuffer.free();
        BasicBuffer buffer = buffers.get(numOfCached - 1);
        buffer.free();
    }
}
