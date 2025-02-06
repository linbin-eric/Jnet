package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NormalAllocateTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("NormalAllocateTest");

    /**
     * 遍历对每层的每一个节点进行分配测试
     */
    @Test
    public void test0()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean direct)
    {
        List<IoBuffer> buffers = new LinkedList<>();
        for (int i = allocator.maxLevel(); i >= 0; i--)
        {
            int levelSize = allocator.pagesize() << (allocator.maxLevel() - i);
            int base      = 1 << i;
            for (int j = 0; j < 1 << i; j++)
            {
                BasicBuffer buffer = (BasicBuffer) allocator.ioBuffer(levelSize, direct);
                long        handle = ((PooledStorageSegment) buffer.getStorageSegment()).getHandle();
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
        test1(true);
        test1(false);
    }

    private void test1(boolean preferDirect)
    {
        int pagesize = allocator.pagesize();
        int maxLevel = allocator.maxLevel();
        for (int i = maxLevel; i > 0; i--)
        {
            int         size   = pagesize << (maxLevel - i);
            BasicBuffer buffer = (BasicBuffer) allocator.ioBuffer(size, preferDirect);
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
