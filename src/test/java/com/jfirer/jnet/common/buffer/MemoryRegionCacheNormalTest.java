package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.CachedPooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkList;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

@RunWith(Parameterized.class)
public class MemoryRegionCacheNormalTest
{
    private long c100Offset = UNSAFE.getFieldOffset("c100", Arena.class);
    private long c075Offset = UNSAFE.getFieldOffset("c075", Arena.class);
    private long c050Offset = UNSAFE.getFieldOffset("c050", Arena.class);
    private long c025Offset = UNSAFE.getFieldOffset("c025", Arena.class);
    private long c000Offset = UNSAFE.getFieldOffset("c000", Arena.class);
    private long cIntOffset = UNSAFE.getFieldOffset("cInt", Arena.class);
    CachedPooledBufferAllocator allocator = new CachedPooledBufferAllocator("test");
    private boolean preferDirect;

    public MemoryRegionCacheNormalTest(boolean preferDirect)
    {
        this.preferDirect = preferDirect;
    }

    @Parameterized.Parameters
    public static Collection<Boolean> data()
    {
        Set<Boolean> set = new HashSet<>();
        set.add(true);
        set.add(false);
        return set;
    }

    @Test
    public void test()
    {
        Queue<IoBuffer> queue = new LinkedList<>();
        queue.add(allocator.ioBuffer(allocator.pagesize(), preferDirect));
        Arena         arena     = (Arena) allocator.currentArena(preferDirect);
        ChunkList     chunkList = (ChunkList) UNSAFE.getObject(arena, cIntOffset);
        int           total     = 1 << allocator.maxLevel();
        ChunkListNode head      = chunkList.head();
        for (int i = 1; i < total; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize(), preferDirect));
            Assert.assertEquals((total - i - 1) * allocator.pagesize(), head.getFreeBytes());
        }
        Assert.assertEquals(0, head.getFreeBytes());
        for (int i = 0; i < CachedPooledBufferAllocator.NUM_OF_CACHE; i++)
        {
            queue.poll().free();
            Assert.assertEquals(0, head.getFreeBytes());
        }
        queue.poll().free();
        Assert.assertEquals(allocator.pagesize(), head.getFreeBytes());
        queue.forEach(IoBuffer::free);
        Assert.assertEquals((total - CachedPooledBufferAllocator.NUM_OF_CACHE) * allocator.pagesize(), head.getFreeBytes());
    }
}
