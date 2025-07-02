package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator2;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer2;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NormalAllocateTest
{
    PooledBufferAllocator2 allocatorHeap   = new PooledBufferAllocator2(100, false, new Arena("heap", BufferType.HEAP));
    PooledBufferAllocator2 allocatorDirect = new PooledBufferAllocator2(100, true, new Arena("direct", BufferType.UNSAFE));

    /**
     * 遍历对每层的每一个节点进行分配测试
     */
    @Test
    public void test0()
    {
        test0(allocatorHeap);
        test0(allocatorDirect);
    }

    private void test0(PooledBufferAllocator2 allocator)
    {
        List<IoBuffer> buffers = new LinkedList<>();
        for (int i = PooledBufferAllocator2.MAXLEVEL; i >= 0; i--)
        {
            int levelSize = PooledBufferAllocator2.PAGESIZE << (PooledBufferAllocator2.MAXLEVEL - i);
            int base      = 1 << i;
            for (int j = 0; j < 1 << i; j++)
            {
                PooledBuffer2 buffer = (PooledBuffer2) allocator.allocate(levelSize);
                long          handle = (buffer).getHandle();
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

    /**
     * 每一层都尝试获取该层的节点大小
     */
    @Test
    public void test1()
    {
        test1(allocatorHeap);
        test1(allocatorDirect);
    }

    private void test1(PooledBufferAllocator2 allocator)
    {
        int pagesize = PooledBufferAllocator2.PAGESIZE;
        int maxLevel = PooledBufferAllocator2.MAXLEVEL;
        for (int i = maxLevel; i > 0; i--)
        {
            int           size   = pagesize << (maxLevel - i);
            PooledBuffer2 buffer = (PooledBuffer2) allocator.allocate(size);
            if (i == maxLevel)
            {
                assertEquals(1 << i, (buffer).getHandle());
            }
            else
            {
                assertEquals((1L << i) + 1, (buffer).getHandle());
            }
        }
    }
}
