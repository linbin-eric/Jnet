package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;
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
    long                  subPagesOffset     = UNSAFE.getFieldOffset("subPages", ChunkImpl.class);
    long                  bitMapOffset       = UNSAFE.getFieldOffset("bitMap", SubPageImpl.class);

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
        int               pagesize     = allocator.pagesize;
        int               elementNum   = pagesize / reqCapacity;
        int               numOfSubPage = 1 << allocator.maxLevel;
        ChunkImpl<?>      chunk        = null;
        AbstractArena<?>  arena        = (AbstractArena<?>) allocator.currentArena(direct);
        Queue<IoBuffer>   buffers      = new LinkedList<>();
        Queue<SubPage>    subPageQueue = new LinkedList<>();
        SubPageListNode[] subPageHeads = (SubPageListNode[]) UNSAFE.getObject(arena, subPageHeadsOffset);
        SubPageListNode   head         = subPageHeads[MathUtil.log2(reqCapacity) - 4];
        for (int i = 0; i < numOfSubPage; i++)
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
                    chunk = arena.cInt.head;
                }
                if (elementIdx != elementNum - 1)
                {
                    assertTrue(head.getNext().getSubPage() == ((SubPage[]) UNSAFE.getObject(chunk, subPagesOffset))[i]);
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
                    assertTrue("当前下标" + i, head.getNext().getSubPage() == subPages[i]);
                    SubPageListNode subPageListNode = ((ChunkListNode<?>) chunk).find(i);
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
//    @Test
//    public void test1()
//    {
//        test1(true);
//        test1(false);
//    }
//
//    private void test1(boolean preferDirect)
//    {
//        Queue<IoBuffer>  buffers    = new LinkedList<>();
//        int              elementNum = allocator.pagesize / 512;
//        AbstractArena<?> arena      = allocator.threadCache().arena(preferDirect);
//        SubPageImpl<?>   head       = arena.findSubPageHead(512);
//        SubPageImpl<?>   subPage1;
//        IoBuffer         buffer     = allocator.ioBuffer(512, preferDirect);
//        buffers.add(buffer);
//        subPage1 = head.next;
//        assertTrue(subPage1.next == head && subPage1.prev == head);
//        for (int i = 1; i < elementNum; i++)
//        {
//            buffer = allocator.ioBuffer(512, preferDirect);
//            buffers.add(buffer);
//        }
//        assertNull(subPage1.prev);
//        assertNull(subPage1.next);
//        assertTrue(head == head.next);
//        buffer = allocator.ioBuffer(512, preferDirect);
//        buffers.add(buffer);
//        SubPageImpl<?> subPage2 = head.next;
//        assertTrue(subPage2.next == head && subPage2.prev == head);
//        for (int i = 1; i < elementNum; i++)
//        {
//            buffer = allocator.ioBuffer(512, preferDirect);
//            buffers.add(buffer);
//        }
//        assertNull(subPage2.prev);
//        assertNull(subPage2.next);
//        assertTrue(head == head.next);
//        while (buffers.isEmpty() == false)
//        {
//            buffers.poll().free();
//        }
//        assertTrue(head.next == subPage1);
//        assertNull(subPage2.prev);
//        assertNull(subPage2.next);
//        for (int i = 0; i < elementNum; i++)
//        {
//            buffer = allocator.ioBuffer(512, preferDirect);
//            buffers.add(buffer);
//        }
//        assertTrue(head.next == head && subPage1.prev == null && subPage1.next == null);
//        allocator.ioBuffer(512, preferDirect);
//        assertTrue(head.next == subPage2);
//    }
}
