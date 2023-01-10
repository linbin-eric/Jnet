package com.jfirer.jnet.common.buffer;

import org.junit.Ignore;

@Ignore
public class TinyAllocateTest
{
//    PooledBufferAllocator allocator = new PooledUnThreadCacheBufferAllocator("test");
//    int                   reqCapacity;
//
//    public TinyAllocateTest(int reqCapacity)
//    {
//        this.reqCapacity = reqCapacity;
//    }
//
//    @Parameters
//    public static List<Integer> params()
//    {
//        List<Integer> list = new LinkedList<>();
//        int           i    = 16;
//        while (i < 512)
//        {
//            list.add(i);
//            i += 16;
//        }
//        return list;
//    }
//
//    @Test
//    public void test()
//    {
//        test0(true);
//        test0(false);
//    }
//
//    private void test0(boolean direct)
//    {
//        int                   pagesize   = allocator.pagesize;
//        int                   elementNum = pagesize / reqCapacity;
//        int                   numPage    = 1 << allocator.maxLevel;
//        ChunkImpl<?>          chunk      = null;
//        AbstractArena<?>      arena      = allocator.threadCache().arena(direct);
//        Queue<IoBuffer>       buffers    = new LinkedList<>();
//        Queue<SubPageImpl<?>> subPages   = new LinkedList<>();
//        SubPageImpl<?>        head       = arena.findSubPageHead(reqCapacity);
//        for (int i = 0; i < numPage; i++)
//        {
//            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
//            {
//                AbstractBuffer<?> buffer = (AbstractBuffer<?>) allocator.ioBuffer(reqCapacity, direct);
//                buffers.add((IoBuffer) buffer);
//                int offset = i * pagesize + elementIdx * reqCapacity;
//                assertEquals(reqCapacity, buffer.capacity);
//                assertEquals(offset, buffer.offset);
//                if (chunk == null)
//                {
//                    chunk = allocator.threadCache().arena(direct).cInt.head;
//                }
//                if (elementIdx != elementNum - 1)
//                {
//                    assertTrue(head.next == chunk.subPages[i]);
//                }
//                else
//                {
//                    assertTrue(head.next == head);
//                }
//            }
//            subPages.offer(chunk.subPages[i]);
//            assertEquals(0, chunk.subPages[i].numAvail);
//        }
//        for (int i = 0; i < numPage; i++)
//        {
//            SubPageImpl<?> subPage = chunk.subPages[i];
//            long[]         bitMap  = subPage.bitMap;
//            for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
//            {
//                buffers.poll().free();
//                if (elementIdx != elementNum - 1)
//                {
//                    assertTrue("当前下标" + i, head.next == chunk.subPages[i]);
//                    if (i == 0)
//                    {
//                        assertTrue(chunk.subPages[i].next == head);
//                    }
//                    else
//                    {
//                        assertTrue(chunk.subPages[i].next == chunk.subPages[0]);
//                    }
//                }
//                else
//                {
//                    assertTrue(head.next == chunk.subPages[0]);
//                }
//                assertTrue(chunk.subPages[0].next == head);
//                int r = elementIdx >>> 6;
//                int j = elementIdx & 63;
//                assertEquals(0, (bitMap[r] >>> j) & 1);
//                if (j != 63 && elementIdx != elementNum - 1)
//                {
//                    j++;
//                    assertNotEquals("出现问题的坐标" + j, 0, (bitMap[r] >>> j) & 1);
//                }
//            }
//        }
//    }
}
