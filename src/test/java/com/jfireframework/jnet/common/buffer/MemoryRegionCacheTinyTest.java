package com.jfireframework.jnet.common.buffer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MemoryRegionCacheTinyTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("test");
    private int size;

    public MemoryRegionCacheTinyTest(int size)
    {
        this.size = size;
    }

    @Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           i    = 16;
        while (i < 512)
        {
            list.add(i);
            i += 16;
        }
        return list;
    }

    @Test
    public void tinyTest() throws InterruptedException
    {
        test0(false, size);
        test0(true, size);
    }

    @SuppressWarnings("unchecked")
    private void test0(boolean preferDirect, int size) throws InterruptedException
    {
        int                   tinyCacheSize = allocator.tinyCacheSize;
        final Queue<IoBuffer> buffers       = new LinkedList<>();
        Set<Chunk<?>>         chunks        = new HashSet<>();
        for (int i = 0; i < tinyCacheSize; i++)
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
        @SuppressWarnings("rawtypes") MemoryRegionCache memoryRegionCache = threadCache.findCache(size, SizeType.TINY, threadCache.arena(preferDirect));
        assertEquals(tinyCacheSize, memoryRegionCache.size());
        assertFalse(memoryRegionCache.offer(chunk, -1L));
        for (int i = 0; i < tinyCacheSize; i++)
        {
            PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(size, preferDirect);
            buffers.add(buffer);
        }
        assertEquals(freeBytes, chunk.freeBytes);
        assertTrue(memoryRegionCache.isEmpty());
    }
}
