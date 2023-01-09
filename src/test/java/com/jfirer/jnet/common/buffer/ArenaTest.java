package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class ArenaTest
{
    private PooledBufferAllocator allocator;

    public ArenaTest()
    {
        allocator = new PooledUnThreadCacheBufferAllocator("test");
    }

    /**
     * 测试随着不同申请率，在多个ChunkList进行移动
     */
    @Test
    public void test()
    {
        testMove(true);
        testMove(false);
    }

    private void testMove(boolean preferDirect)
    {
        IoBuffer        buffer = allocator.ioBuffer(allocator.pagesize, preferDirect);
        Queue<IoBuffer> queue  = new LinkedList<>();
        queue.add(buffer);
        ThreadCache      threadCache = allocator.threadCache();
        AbstractArena<?> arena       = threadCache.arena(preferDirect);
        assertNull(arena.c100.head);
        assertNull(arena.c075.head);
        assertNull(arena.c050.head);
        assertNull(arena.c025.head);
        assertNull(arena.c000.head);
        assertNotNull(arena.cInt.head);
        ChunkImpl<?> chunk = arena.cInt.head;
        assertEquals(1 << (allocator.maxLevel + 1), chunk.allocationCapacity.length);
        int total   = 1 << allocator.maxLevel;
        int quarter = total >>> 2;
        for (int i = 1; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize, preferDirect));
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNotNull(arena.cInt.head);
            assertTrue(chunk == arena.cInt.head);
        }
        assertEquals(25, chunk.usage());
        for (int i = 0; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize, preferDirect));
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNotNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c000.head);
        }
        assertEquals(50, chunk.usage());
        for (int i = 0; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize, preferDirect));
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNotNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c025.head);
        }
        assertEquals(75, chunk.usage());
        for (int i = 0; i < quarter - 1; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize, preferDirect));
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNotNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c050.head);
        }
        assertEquals(99, chunk.usage());
        queue.add(allocator.ioBuffer(allocator.pagesize, preferDirect));
        assertNotNull(arena.c100.head);
        assertNull(arena.c075.head);
        assertNull(arena.c050.head);
        assertNull(arena.c025.head);
        assertNull(arena.c000.head);
        assertNull(arena.cInt.head);
        assertTrue(chunk == arena.c100.head);
        assertEquals(100, chunk.usage());
        queue.poll().free();
        assertEquals(99, chunk.usage());
        assertNull(arena.c100.head);
        assertNotNull(arena.c075.head);
        assertNull(arena.c050.head);
        assertNull(arena.c025.head);
        assertNull(arena.c000.head);
        assertNull(arena.cInt.head);
        assertTrue(chunk == arena.c075.head);
        for (int i = 1; i < quarter; i++)
        {
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNotNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c075.head);
        }
        assertEquals(75, chunk.usage());
        int percent = (1 << allocator.maxLevel) / 100;
        for (int i = 0; i < percent; i++)
        {
            assertEquals(75, chunk.usage());
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNotNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c075.head);
        }
        for (int i = 0; i < quarter - percent; i++)
        {
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNotNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c050.head);
        }
        assertEquals(50, chunk.usage());
        for (int i = 0; i < percent; i++)
        {
            assertEquals(50, chunk.usage());
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNotNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c050.head);
        }
        for (int i = 0; i < quarter - percent; i++)
        {
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNotNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c025.head);
        }
        assertEquals(25, chunk.usage());
        for (int i = 0; i < percent; i++)
        {
            assertEquals(25, chunk.usage());
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNotNull(arena.c025.head);
            assertNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c025.head);
        }
        for (int i = 0; i < quarter - percent - 1; i++)
        {
            queue.poll().free();
            assertNull(arena.c100.head);
            assertNull(arena.c075.head);
            assertNull(arena.c050.head);
            assertNull(arena.c025.head);
            assertNotNull(arena.c000.head);
            assertNull(arena.cInt.head);
            assertTrue(chunk == arena.c000.head);
        }
        assertEquals(1, chunk.usage());
        queue.poll().free();
        assertTrue(queue.isEmpty());
        assertNull(arena.c100.head);
        assertNull(arena.c075.head);
        assertNull(arena.c050.head);
        assertNull(arena.c025.head);
        assertNull(arena.c000.head);
        assertNull(arena.cInt.head);
    }
}
