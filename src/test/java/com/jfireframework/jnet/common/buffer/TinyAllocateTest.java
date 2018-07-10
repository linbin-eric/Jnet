package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.baseutil.time.Timewatch;

@RunWith(Parameterized.class)
public class TinyAllocateTest
{
	PooledBufferAllocator	allocator	= new PooledUnThreadCacheBufferAllocator();
	int						reqCapacity;
	
	@Parameters
	public static List<Integer> params()
	{
		List<Integer> list = new LinkedList<>();
		int i = 16;
		while (i < 512)
		{
			list.add(i);
			i += 16;
		}
		return list;
	}
	
	public TinyAllocateTest(int reqCapacity)
	{
		this.reqCapacity = reqCapacity;
	}
	
	@Test
	public void test()
	{
		Timewatch timewatch = new Timewatch();
		test0(true);
		timewatch.end();
		System.out.println("堆外内存耗时:" + timewatch.getTotal());
		timewatch.start();
		test0(false);
		timewatch.end();
		System.out.println("堆内存耗时:" + timewatch.getTotal());
	}
	
	private void test0(boolean direct)
	{
		int pagesize = allocator.pagesize;
		int elementNum = pagesize / reqCapacity;
		int numPage = 1 << allocator.maxLevel;
		Chunk<?> chunk = null;
		Arena<?> arena = allocator.threadCache().arena(direct);
		Queue<IoBuffer> buffers = new LinkedList<>();
		Queue<SubPage<?>> subPages = new LinkedList<>();
		SubPage<?> head = arena.findSubPageHead(reqCapacity);
		Timewatch timewatch = new Timewatch();
		for (int i = 0; i < numPage; i++)
		{
			for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
			{
				PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(reqCapacity, direct);
				buffers.add(buffer);
				int offset = i * pagesize + elementIdx * reqCapacity;
				assertEquals(reqCapacity, buffer.capacity);
				assertEquals(offset, buffer.offset);
				if (chunk == null)
				{
					chunk = allocator.threadCache().arena(direct).cInt.head;
				}
				if (elementIdx != elementNum - 1)
				{
					assertTrue(head.next == chunk.subPages[i]);
				}
				else
				{
					assertTrue(head.next == head);
				}
			}
			subPages.offer(chunk.subPages[i]);
			assertEquals(0, chunk.subPages[i].numAvail);
		}
		timewatch.end();
		System.out.println("分配耗时:" + timewatch.getTotal());
		timewatch.start();
		for (int i = 0; i < numPage; i++)
		{
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
			}
		}
		timewatch.end();
		System.out.println("回收耗时:" + timewatch.getTotal());
	}
}
