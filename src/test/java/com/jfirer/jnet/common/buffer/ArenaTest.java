package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkList;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class ArenaTest
{
    private final PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator("testHeap", false);
    private final PooledBufferAllocator allocatorDirect = new PooledBufferAllocator("test", true);
    private final long                  c100Offset      = UNSAFE.getFieldOffset("c100", Arena.class);
    private final long                  c075Offset      = UNSAFE.getFieldOffset("c075", Arena.class);
    private final long                  c050Offset      = UNSAFE.getFieldOffset("c050", Arena.class);
    private final long                  c025Offset      = UNSAFE.getFieldOffset("c025", Arena.class);
    private final long                  c000Offset      = UNSAFE.getFieldOffset("c000", Arena.class);
    private final long                  cIntOffset      = UNSAFE.getFieldOffset("cInt", Arena.class);

    public ArenaTest()
    {
    }

    /**
     * 测试随着不同申请率，在多个ChunkList进行移动
     */
    @Test
    public void test()
    {
        testMove(allocatorHeap);
        testMove(allocatorDirect);
    }

    private void testMove(PooledBufferAllocator allocator)
    {
        IoBuffer        buffer = allocator.ioBuffer(allocator.pagesize());
        Queue<IoBuffer> queue  = new LinkedList<>();
        queue.add(buffer);
//        ThreadCache      threadCache = allocator.threadCache();
        Arena     arena = allocator.currentArena();
        ChunkList c100  = (ChunkList) UNSAFE.getObject(arena, c100Offset);
        ChunkList c075  = (ChunkList) UNSAFE.getObject(arena, c075Offset);
        ChunkList c050  = (ChunkList) UNSAFE.getObject(arena, c050Offset);
        ChunkList c025  = (ChunkList) UNSAFE.getObject(arena, c025Offset);
        ChunkList c000  = (ChunkList) UNSAFE.getObject(arena, c000Offset);
        ChunkList cInt  = (ChunkList) UNSAFE.getObject(arena, cIntOffset);
        assertNull(c100.head());
        assertNull(c075.head());
        assertNull(c050.head());
        assertNull(c025.head());
        assertNull(c000.head());
        assertNotNull(cInt.head());
        ChunkListNode chunkListNode = cInt.head();
        int           total         = 1 << allocator.maxLevel();
        int           quarter       = total >>> 2;
        for (int i = 1; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize()));
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNull(c025.head());
            assertNull(c000.head());
            assertNotNull(cInt.head());
            assertSame(chunkListNode, cInt.head());
        }
        assertEquals(25, chunkListNode.usage());
        for (int i = 0; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize()));
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNull(c025.head());
            assertNotNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c000.head());
        }
        assertEquals(50, chunkListNode.usage());
        for (int i = 0; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize()));
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNotNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c025.head());
        }
        assertEquals(75, chunkListNode.usage());
        for (int i = 1; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(allocator.pagesize()));
            assertNull(c100.head());
            if (chunkListNode.usage() <= 90)
            {
                assertNull(c075.head());
                assertNotNull(c050.head());
            }
            else
            {
                assertNotNull(c075.head());
                assertNull(c050.head());
            }
            assertNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            if (chunkListNode.usage() <= 90)
            {
                assertSame(chunkListNode, c050.head());
            }
            else
            {
                assertSame(chunkListNode, c075.head());
            }
        }
        assertEquals(99, chunkListNode.usage());
        queue.add(allocator.ioBuffer(allocator.pagesize()));
        assertNotNull(c100.head());
        assertNull(c075.head());
        assertNull(c050.head());
        assertNull(c025.head());
        assertNull(c000.head());
        assertNull(cInt.head());
        assertSame(chunkListNode, c100.head());
        assertEquals(100, chunkListNode.usage());
        queue.poll().free();
        assertEquals(99, chunkListNode.usage());
        assertNull(c100.head());
        assertNotNull(c075.head());
        assertNull(c050.head());
        assertNull(c025.head());
        assertNull(c000.head());
        assertNull(cInt.head());
        assertSame(chunkListNode, c075.head());
        for (int i = 1; i < quarter; i++)
        {
            queue.poll().free();
            assertNull(c100.head());
            assertNotNull(c075.head());
            assertNull(c050.head());
            assertNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c075.head());
        }
        assertEquals(75, chunkListNode.usage());
        int percent = (1 << allocator.maxLevel()) / 100;
        for (int i = 0; i < percent; i++)
        {
            assertEquals(75, chunkListNode.usage());
            queue.poll().free();
            assertNull(c100.head());
            assertNotNull(c075.head());
            assertNull(c050.head());
            assertNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c075.head());
        }
        for (int i = 0; i < quarter - percent; i++)
        {
            queue.poll().free();
            assertNull(c100.head());
            assertNull(c075.head());
            assertNotNull(c050.head());
            assertNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c050.head());
        }
        assertEquals(50, chunkListNode.usage());
        for (int i = 0; i < percent; i++)
        {
            assertEquals(50, chunkListNode.usage());
            queue.poll().free();
            assertNull(c100.head());
            assertNull(c075.head());
            assertNotNull(c050.head());
            assertNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c050.head());
        }
        for (int i = 0; i < quarter - percent; i++)
        {
            queue.poll().free();
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNotNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c025.head());
        }
        assertEquals(25, chunkListNode.usage());
        for (int i = 0; i < percent; i++)
        {
            assertEquals(25, chunkListNode.usage());
            queue.poll().free();
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNotNull(c025.head());
            assertNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c025.head());
        }
        for (int i = 0; i < quarter - percent - 1; i++)
        {
            queue.poll().free();
            assertNull(c100.head());
            assertNull(c075.head());
            assertNull(c050.head());
            assertNull(c025.head());
            assertNotNull(c000.head());
            assertNull(cInt.head());
            assertSame(chunkListNode, c000.head());
        }
        assertEquals(1, chunkListNode.usage());
        queue.poll().free();
        assertTrue(queue.isEmpty());
        assertNull(c100.head());
        assertNull(c075.head());
        assertNull(c050.head());
        assertNull(c025.head());
        assertNull(c000.head());
        assertNull(cInt.head());
    }
}
