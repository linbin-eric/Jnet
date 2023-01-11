package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.impl.AbstractArena;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkImpl;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkList;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class ChunkListTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("test");
    private long c100Offset = UNSAFE.getFieldOffset("c100", AbstractArena.class);
    private long c075Offset = UNSAFE.getFieldOffset("c075", AbstractArena.class);
    private long c050Offset = UNSAFE.getFieldOffset("c050", AbstractArena.class);
    private long c025Offset = UNSAFE.getFieldOffset("c025", AbstractArena.class);
    private long c000Offset = UNSAFE.getFieldOffset("c000", AbstractArena.class);
    private long cIntOffset = UNSAFE.getFieldOffset("cInt", AbstractArena.class);

    @Test
    public void test0()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean preferDirect)
    {
        int             chunkSize = allocator.pagesize << allocator.maxLevel;
        int             size      = chunkSize >> 2;
        Queue<IoBuffer> buffers   = new LinkedList<>();
        for (int i = 0; i < 4; i++)
        {
            IoBuffer buffer = allocator.ioBuffer(size, preferDirect);
            if (i != 3)
            {
                buffers.add(buffer);
            }
        }
        AbstractArena<?> arena  = (AbstractArena<?>) allocator.currentArena(preferDirect);
        ChunkList<?>     c100   = (ChunkList<?>) UNSAFE.getObject(arena, c100Offset);
        ChunkListNode    chunk1 = c100.head();
        for (int i = 0; i < 4; i++)
        {
            IoBuffer buffer = allocator.ioBuffer(size, preferDirect);
            if (i != 3)
            {
                buffers.add(buffer);
            }
        }
        ChunkListNode chunk2 = c100.head();
        assertTrue(chunk1 != chunk2);
        while (buffers.isEmpty() == false)
        {
            buffers.poll().free();
        }
        assertTrue(chunk2.getNext() == chunk1);
        allocator.ioBuffer(size, preferDirect);
        allocator.ioBuffer(size, preferDirect);
        assertEquals(75, chunk2.usage());
        allocator.ioBuffer(size << 1, preferDirect);
        assertEquals(75, chunk1.usage());
        allocator.ioBuffer(size << 1, preferDirect);
        ChunkList    c000   = (ChunkList) UNSAFE.getObject(arena, c000Offset);
        ChunkImpl<?> chunk3 = c000.head();
        assertNotNull(chunk3);
        assertEquals(50, chunk3.usage());
    }
}
