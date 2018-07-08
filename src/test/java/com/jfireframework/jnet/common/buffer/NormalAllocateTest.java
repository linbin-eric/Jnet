package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

public class NormalAllocateTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator(PooledBufferAllocator.PAGESIZE, PooledBufferAllocator.MAXLEVEL, 1, 1, 0, 0, 0, 0, false, true);
    
    @Test
    public void test()
    {
        test0(true);
        test0(false);
    }
    
    private void test0(boolean direct)
    {
        List<IoBuffer> buffers = new LinkedList<>();
        for (int i = allocator.maxLevel; i >= 0; i--)
        {
            int levelSize = allocator.pagesize << (allocator.maxLevel - i);
            int base = 1 << i;
            for (int j = 0; j < 1 << i; j++)
            {
                PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(levelSize, direct);
                long handle = buffer.handle;
                assertEquals(base + j, handle);
                buffers.add(buffer);
            }
            for (IoBuffer each : buffers)
            {
                each.free();
            }
            buffers.clear();
        }
    }
}
