package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator2;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkList;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ArenaTest
{
    private final long                  c100Offset      = UNSAFE.getFieldOffset("c100", Arena.class);
    private final long                  c075Offset      = UNSAFE.getFieldOffset("c075", Arena.class);
    private final long                  c050Offset      = UNSAFE.getFieldOffset("c050", Arena.class);
    private final long                  c025Offset      = UNSAFE.getFieldOffset("c025", Arena.class);
    private final long                  c000Offset      = UNSAFE.getFieldOffset("c000", Arena.class);
    private final long                  cIntOffset      = UNSAFE.getFieldOffset("cInt", Arena.class);
    private       BufferAllocator       allocator;

    public ArenaTest(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Parameterized.Parameters
    public static Collection<?> data()
    {
        return List.of(new Object[][]{//
                {new PooledBufferAllocator("test", true)},//
                {new PooledBufferAllocator("test", false)},//
                {new PooledBufferAllocator2(100,true,new Arena(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, "test", BufferType.UNSAFE))},//
                {new PooledBufferAllocator2(100,false,new Arena(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, "test", BufferType.HEAP))}//
        });
    }

    /**
     * 测试随着不同申请率，在多个ChunkList进行移动
     */
    @Test
    public void test()
    {
        testMove(allocator);
    }

    private Arena findArean(BufferAllocator bufferAllocator)
    {
        if (bufferAllocator instanceof PooledBufferAllocator pooledBufferAllocator)
        {
            return pooledBufferAllocator.currentArena();
        }
        else if (bufferAllocator instanceof PooledBufferAllocator2 pooledBufferAllocator2)
        {
            return pooledBufferAllocator2.getArena();
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    private void testMove(BufferAllocator allocator)
    {
        IoBuffer        buffer = allocator.ioBuffer(PooledBufferAllocator.PAGESIZE);
        Queue<IoBuffer> queue  = new LinkedList<>();
        queue.add(buffer);
//        ThreadCache      threadCache = allocator.threadCache();
        Arena     arena = findArean(allocator);
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
        int           total         = 1 << PooledBufferAllocator.MAXLEVEL;
        int           quarter       = total >>> 2;
        for (int i = 1; i < quarter; i++)
        {
            queue.add(allocator.ioBuffer(PooledBufferAllocator.PAGESIZE));
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
            queue.add(allocator.ioBuffer(PooledBufferAllocator.PAGESIZE));
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
            queue.add(allocator.ioBuffer(PooledBufferAllocator.PAGESIZE));
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
            queue.add(allocator.ioBuffer(PooledBufferAllocator.PAGESIZE));
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
        queue.add(allocator.ioBuffer(PooledBufferAllocator.PAGESIZE));
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
        int percent = (1 << PooledBufferAllocator.MAXLEVEL) / 100;
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
