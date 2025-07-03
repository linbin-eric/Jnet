package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NormalAllocateTest
{
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator(100, false, new Arena("heap", BufferType.HEAP));
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator(100, true, new Arena("direct", BufferType.UNSAFE));

    /**
     * 遍历对每层的每一个节点进行分配测试
     */
    @Test
    public void test0()
    {
        test0(allocatorHeap);
        test0(allocatorDirect);
    }

    private void test0(PooledBufferAllocator allocator)
    {
        List<IoBuffer> buffers = new LinkedList<>();
        for (int i = PooledBufferAllocator.MAXLEVEL; i >= 0; i--)
        {
            int levelSize = PooledBufferAllocator.PAGESIZE << (PooledBufferAllocator.MAXLEVEL - i);
            int base      = 1 << i;
            for (int j = 0; j < 1 << i; j++)
            {
                PooledBuffer buffer = (PooledBuffer) allocator.allocate(levelSize);
                long         handle = (buffer).getHandle();
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

    private void test1(PooledBufferAllocator allocator)
    {
        int pagesize = PooledBufferAllocator.PAGESIZE;
        int maxLevel = PooledBufferAllocator.MAXLEVEL;
        for (int i = maxLevel; i > 0; i--)
        {
            int          size   = pagesize << (maxLevel - i);
            PooledBuffer buffer = (PooledBuffer) allocator.allocate(size);
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
