package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.impl.AbstractArena;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkList;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.arena.impl.SubPage;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SmallAllocateTest
{
    PooledBufferAllocator allocator          = new PooledBufferAllocator("test");
    int                   reqCapacity;
    long                  subPageHeadsOffset = UNSAFE.getFieldOffset("subPageHeads", AbstractArena.class);
    long                  subPagesOffset     = UNSAFE.getFieldOffset("subPages", ChunkListNode.class);
    long                  bitMapOffset       = UNSAFE.getFieldOffset("bitMap", SubPage.class);
    private long c100Offset = UNSAFE.getFieldOffset("c100", AbstractArena.class);
    private long c075Offset = UNSAFE.getFieldOffset("c075", AbstractArena.class);
    private long c050Offset = UNSAFE.getFieldOffset("c050", AbstractArena.class);
    private long c025Offset = UNSAFE.getFieldOffset("c025", AbstractArena.class);
    private long c000Offset = UNSAFE.getFieldOffset("c000", AbstractArena.class);
    private long cIntOffset = UNSAFE.getFieldOffset("cInt", AbstractArena.class);

    public SmallAllocateTest(int reqCapacity)
    {
        this.reqCapacity = reqCapacity;
    }

    @Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           size = 16;
        while (size < PooledBufferAllocator.PAGESIZE)
        {
            list.add(size);
            size <<= 1;
        }
        return list;
    }

    @Test
    public void test0()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean direct)
    {
        int                pagesize     = allocator.pagesize();
        int                elementNum   = pagesize / reqCapacity;
        int                numOfSubPage = 1 << allocator.maxLevel();
        ChunkListNode<?>   chunk        = null;
        AbstractArena<?>   arena        = (AbstractArena<?>) allocator.currentArena(direct);
        Queue<IoBuffer<?>> buffers      = new LinkedList<>();
        Queue<SubPage<?>>  subPageQueue = new LinkedList<>();
        SubPage<?>[]       subPageHeads = (SubPage<?>[]) UNSAFE.getObject(arena, subPageHeadsOffset);
        SubPage<?>         head         = subPageHeads[MathUtil.log2(reqCapacity) - 4];
        ChunkList<?>       cInt         = (ChunkList<?>) UNSAFE.getObject(arena, cIntOffset);
        for (int i = 0; i < numOfSubPage; i++)
        {
            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
            {
                AbstractBuffer<?> buffer = (AbstractBuffer<?>) allocator.ioBuffer(reqCapacity, direct);
                buffers.add((IoBuffer) buffer);
                int offset = i * pagesize + elementIdx * reqCapacity;
                assertEquals(reqCapacity, buffer.capacity());
                assertEquals(offset, buffer.offset());
                if (chunk == null)
                {
                    chunk = cInt.head();
                }
                if (elementIdx != elementNum - 1)
                {
                    assertTrue(head.getNext() == ((SubPage[]) UNSAFE.getObject(chunk, subPagesOffset))[i]);
                }
                else
                {
                    assertTrue(head.getNext() == head);
                }
            }
            subPageQueue.offer(((SubPage[]) UNSAFE.getObject(chunk, subPagesOffset))[i]);
            assertEquals(0, ((SubPage[]) UNSAFE.getObject(chunk, subPagesOffset))[i].numOfAvail());
        }
        SubPage[] subPages = (SubPage[]) UNSAFE.getObject(chunk, subPagesOffset);
        for (int i = 0; i < numOfSubPage; i++)
        {
            SubPage subPage = subPages[i];
            long[]  bitMap  = (long[]) UNSAFE.getObject(subPage, bitMapOffset);
            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
            {
                buffers.poll().free();
                if (elementIdx != elementNum - 1)
                {
                    assertTrue("当前下标" + i, head.getNext() == subPages[i]);
                    SubPage subPageListNode = ((ChunkListNode<?>) chunk).find(i);
                    if (i == 0)
                    {
                        assertTrue(subPageListNode.getNext() == head);
                    }
                    else
                    {
                        assertTrue("当前下标" + i, subPageListNode.getNext() == ((ChunkListNode<?>) chunk).find(0));
                    }
                }
                else
                {
                    assertTrue(head.getNext() == ((ChunkListNode<?>) chunk).find(0));
                }
                assertTrue(((ChunkListNode<?>) chunk).find(0).getNext() == head);
                int r = elementIdx >>> 6;
                int j = elementIdx & 63;
                assertEquals(0, (bitMap[r] >>> j) & 1);
                if (j != 63 && elementIdx != elementNum - 1)
                {
                    j++;
                    assertNotEquals("出现问题的坐标" + j, 0, (bitMap[r] >>> j) & 1);
                }
            }
        }
        while (buffers.isEmpty() == false)
        {
            buffers.poll().free();
        }
        assertTrue(head.getNext() != head);
    }
}
