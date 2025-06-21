package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NormalAllocateTest
{
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator("NormalAllocateTest", false);
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator("NormalAllocateTest", true);

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
        for (int i = allocator.maxLevel(); i >= 0; i--)
        {
            int levelSize = allocator.pagesize() << (allocator.maxLevel() - i);
            int base      = 1 << i;
            for (int j = 0; j < 1 << i; j++)
            {
                UnPooledBuffer buffer = (UnPooledBuffer) allocator.ioBuffer(levelSize);
                long           handle = ((PooledStorageSegment) buffer.getStorageSegment()).getHandle();
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
        int pagesize = allocator.pagesize();
        int maxLevel = allocator.maxLevel();
        for (int i = maxLevel; i > 0; i--)
        {
            int            size   = pagesize << (maxLevel - i);
            UnPooledBuffer buffer = (UnPooledBuffer) allocator.ioBuffer(size);
            if (i == maxLevel)
            {
                assertEquals(1 << i, ((PooledStorageSegment) buffer.getStorageSegment()).getHandle());
            }
            else
            {
                assertEquals((1L << i) + 1, ((PooledStorageSegment) buffer.getStorageSegment()).getHandle());
            }
        }
    }
}
