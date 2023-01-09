package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;
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
    PooledBufferAllocator allocator = new PooledUnThreadCacheBufferAllocator("test");
    int                   reqCapacity;

    public SmallAllocateTest(int reqCapacity)
    {
        this.reqCapacity = reqCapacity;
    }

    @Parameters
    public static List<Integer> params()
    {
        List<Integer> list = new LinkedList<>();
        int           size = 512;
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
        int                   pagesize   = allocator.pagesize;
        int                   elementNum = pagesize / reqCapacity;
        int                   numPage    = 1 << allocator.maxLevel;
        ChunkImpl<?>          chunk      = null;
        AbstractArena<?>      arena      = allocator.threadCache().arena(direct);
        Queue<IoBuffer>       buffers    = new LinkedList<>();
        Queue<SubPageImpl<?>> subPages   = new LinkedList<>();
        SubPageImpl<?>        head       = arena.findSubPageHead(reqCapacity);
        for (int i = 0; i < numPage; i++)
        {
            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
            {
                AbstractBuffer<?> buffer = (AbstractBuffer<?>) allocator.ioBuffer(reqCapacity, direct);
                buffers.add((IoBuffer) buffer);
                int offset = i * pagesize + elementIdx * reqCapacity;
                assertEquals(reqCapacity, buffer.capacity);
                assertEquals(offset, buffer.offset);
                if (chunk == null)
                {
                    chunk = allocator.threadCache().arena(direct).cInt.head;
                }
                if (elementIdx != elementNum - 1)
                {
                    assertTrue( head.next == chunk.subPages[i]);
                }
                else
                {
                    assertTrue(head.next == head);
                }
            }
            subPages.offer(chunk.subPages[i]);
            assertEquals(0, chunk.subPages[i].numAvail);
        }
        for (int i = 0; i < numPage; i++)
        {
            SubPageImpl<?> subPage = chunk.subPages[i];
            long[]         bitMap  = subPage.bitMap;
            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
            {
                buffers.poll().free();
                if (elementIdx != elementNum - 1)
                {
                    assertTrue("当前下标" + i, head.next == chunk.subPages[i]);
                    if (i == 0)
                    {
                        assertTrue(chunk.subPages[i].next == head);
                    }
                    else
                    {
                        assertTrue(chunk.subPages[i].next == chunk.subPages[0]);
                    }
                }
                else
                {
                    assertTrue(head.next == chunk.subPages[0]);
                }
                assertTrue(chunk.subPages[0].next == head);
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
        assertTrue(head.next != head);
    }

    @Test
    public void test1()
    {
        test1(true);
        test1(false);
    }

    private void test1(boolean preferDirect)
    {
        Queue<IoBuffer>  buffers    = new LinkedList<>();
        int              elementNum = allocator.pagesize / 512;
        AbstractArena<?> arena      = allocator.threadCache().arena(preferDirect);
        SubPageImpl<?>   head       = arena.findSubPageHead(512);
        SubPageImpl<?>   subPage1;
        IoBuffer         buffer     = allocator.ioBuffer(512, preferDirect);
        buffers.add(buffer);
        subPage1 = head.next;
        assertTrue(subPage1.next == head && subPage1.prev == head);
        for (int i = 1; i < elementNum; i++)
        {
            buffer = allocator.ioBuffer(512, preferDirect);
            buffers.add(buffer);
        }
        assertNull(subPage1.prev);
        assertNull(subPage1.next);
        assertTrue(head == head.next);
        buffer = allocator.ioBuffer(512, preferDirect);
        buffers.add(buffer);
        SubPageImpl<?> subPage2 = head.next;
        assertTrue(subPage2.next == head && subPage2.prev == head);
        for (int i = 1; i < elementNum; i++)
        {
            buffer = allocator.ioBuffer(512, preferDirect);
            buffers.add(buffer);
        }
        assertNull(subPage2.prev);
        assertNull(subPage2.next);
        assertTrue(head == head.next);
        while (buffers.isEmpty() == false)
        {
            buffers.poll().free();
        }
        assertTrue(head.next == subPage1);
        assertNull(subPage2.prev);
        assertNull(subPage2.next);
        for (int i = 0; i < elementNum; i++)
        {
            buffer = allocator.ioBuffer(512, preferDirect);
            buffers.add(buffer);
        }
        assertTrue(head.next == head && subPage1.prev == null && subPage1.next == null);
        allocator.ioBuffer(512, preferDirect);
        assertTrue(head.next == subPage2);
    }
}
