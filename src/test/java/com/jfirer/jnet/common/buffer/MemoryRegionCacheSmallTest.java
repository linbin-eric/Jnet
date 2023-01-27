package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.CachedPooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

@RunWith(Parameterized.class)
public class MemoryRegionCacheSmallTest
{
    CachedPooledBufferAllocator allocator = new CachedPooledBufferAllocator("test");
    private int size;

    public MemoryRegionCacheSmallTest(int size)
    {
        this.size = size;
    }

    @Parameterized.Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           i    = 16;
        while (i < CachedPooledBufferAllocator.PAGESIZE)
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
        int                   smallCacheSize = CachedPooledBufferAllocator.NUM_OF_CACHE;
        int                   useBytes       = smallCacheSize * size;
        final Queue<IoBuffer> buffers        = new LinkedList<>();
        Set<Chunk<?>>         chunkSet       = new HashSet<>();
        for (int i = 0; i < smallCacheSize; i++)
        {
            IoBuffer<?> buffer = allocator.ioBuffer(size, preferDirect);
            buffers.add(buffer);
            chunkSet.add(((PoolableBuffer<?>) buffer).chunk());
        }
        int allChunkUsedBytes = chunkSet.stream().map(chunk -> chunk.getChunkSize() - chunk.getFreeBytes()).reduce(Integer::sum).get();
        Assert.assertTrue("allChunkUsedBytes:" + allChunkUsedBytes + ",useBytes:" + useBytes, allChunkUsedBytes >= useBytes);
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
        Assert.assertEquals(allChunkUsedBytes, chunkSet.stream().map(chunk -> chunk.getChunkSize() - chunk.getFreeBytes()).reduce(Integer::sum).get().intValue());
    }
}
