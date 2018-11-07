package com.jfireframework.jnet.common.buffer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MemoryRegionCacheSmallTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("test");
    private int size;

    public MemoryRegionCacheSmallTest(int size)
    {
        this.size = size;
    }

    @Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           i    = 512;
        while (i < PooledBufferAllocator.PAGESIZE)
        {
            list.add(i);
            i <<= 1;
        }
        return list;
    }

    @Test
    public void test() throws InterruptedException
    {
        test0(false, size);
        test0(true, size);
    }

    @SuppressWarnings("unchecked")
    private void test0(boolean preferDirect, int size) throws InterruptedException
    {
        int                   smallCacheSize = allocator.smallCacheSize;
        final Queue<IoBuffer> buffers        = new LinkedList<>();
        Set<Chunk<?>>         chunks         = new HashSet<>();
        for (int i = 0; i < smallCacheSize; i++)
        {
            PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(size, preferDirect);
            buffers.add(buffer);
            chunks.add(buffer.chunk);
        }
        assertEquals(1, chunks.size());
        Chunk<?> chunk     = chunks.iterator().next();
        int      freeBytes = chunk.freeBytes;
        Thread thread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while (buffers.isEmpty() == false)
                {
                    buffers.poll().free();
                }
            }
        });
        thread.start();
        thread.join();
        assertEquals(freeBytes, chunk.freeBytes);
        ThreadCache threadCache = allocator.threadCache();
        @SuppressWarnings("rawtypes") MemoryRegionCache memoryRegionCache = threadCache.findCache(size, SizeType.SMALL, threadCache.arena(preferDirect));
        assertEquals(smallCacheSize, memoryRegionCache.size());
        assertFalse(memoryRegionCache.offer(chunk, -1L));
        for (int i = 0; i < smallCacheSize; i++)
        {
            PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(size, preferDirect);
            buffers.add(buffer);
        }
        assertEquals(freeBytes, chunk.freeBytes);
        assertTrue(memoryRegionCache.isEmpty());
    }
}
