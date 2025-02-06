package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.ChunkList;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.*;

public class ChunkListTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("test");
    private final long c100Offset = UNSAFE.getFieldOffset("c100", Arena.class);
    private final long c075Offset = UNSAFE.getFieldOffset("c075", Arena.class);
    private final long c050Offset = UNSAFE.getFieldOffset("c050", Arena.class);
    private final long c025Offset = UNSAFE.getFieldOffset("c025", Arena.class);
    private final long c000Offset = UNSAFE.getFieldOffset("c000", Arena.class);
    private final long cIntOffset = UNSAFE.getFieldOffset("cInt", Arena.class);

    @Test
    public void test0()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean preferDirect)
    {
        int             chunkSize = allocator.pagesize() << allocator.maxLevel();
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
        Arena         arena  = allocator.currentArena(preferDirect);
        ChunkList     c100   = (ChunkList) UNSAFE.getObject(arena, c100Offset);
        ChunkListNode chunk1 = c100.head();
        for (int i = 0; i < 4; i++)
        {
            IoBuffer buffer = allocator.ioBuffer(size, preferDirect);
            if (i != 3)
            {
                buffers.add(buffer);
            }
        }
        ChunkListNode chunk2 = c100.head();
        assertNotSame(chunk1, chunk2);
        while (!buffers.isEmpty())
        {
            buffers.poll().free();
        }
        assertSame(chunk2.getNext(), chunk1);
        allocator.ioBuffer(size, preferDirect);
        allocator.ioBuffer(size, preferDirect);
        assertEquals(75, chunk2.usage());
        allocator.ioBuffer(size << 1, preferDirect);
        assertEquals(75, chunk1.usage());
        allocator.ioBuffer(size << 1, preferDirect);
        ChunkList c000   = (ChunkList) UNSAFE.getObject(arena, c000Offset);
        Chunk     chunk3 = c000.head();
        assertNotNull(chunk3);
        assertEquals(50, chunk3.usage());
    }
}
