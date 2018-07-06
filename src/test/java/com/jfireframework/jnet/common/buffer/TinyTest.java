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
import com.jfireframework.baseutil.StringUtil;

@RunWith(Parameterized.class)
public class TinyTest
{
	PooledBufferAllocator	allocator	= new PooledBufferAllocator(PooledBufferAllocator.PAGESIZE, PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.NUM_HEAP_ARENA, PooledBufferAllocator.NUM_DIRECT_ARENA, 0, 0, 0, 0, true);
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
	
	public TinyTest(int reqCapacity)
	{
		this.reqCapacity = reqCapacity;
	}
	
	@Test
	public void test()
	{
		test0(true);
		test0(false);
	}
	
	private void test0(boolean direct)
	{
		int pagesize = allocator.pagesize;
		int elementNum = pagesize / reqCapacity;
		int numPage = 1 << allocator.maxLevel;
		Chunk<?> chunk = null;
		Arena<?> arena;
		if (direct)
		{
			arena = allocator.threadCache().directArena;
		}
		else
		{
			arena = allocator.threadCache().heapArena;
		}
		Queue<IoBuffer> buffers = new LinkedList<>();
		Queue<SubPage<?>> subPages = new LinkedList<>();
		SubPage<?> head = arena.findSubPageHead(reqCapacity);
		for (int i = 0; i < numPage; i++)
		{
			for (int elementIdx = 0; elementIdx < elementNum; elementIdx++)
			{
				PooledBuffer<?> buffer;
				if (direct)
				{
					buffer = (PooledBuffer<?>) allocator.directBuffer(reqCapacity);
				}
				else
				{
					buffer = (PooledBuffer<?>) allocator.heapBuffer(reqCapacity);
				}
				buffers.add(buffer);
				int offset = i * pagesize + elementIdx * reqCapacity;
				assertEquals(reqCapacity, buffer.capacity);
				assertEquals(offset, buffer.offset);
				if (chunk == null)
				{
					if (direct)
					{
						chunk = allocator.threadCache().directArena.cInt.head;
					}
					else
					{
						chunk = allocator.threadCache().heapArena.cInt.head;
					}
				}
				if (elementIdx != elementNum - 1)
				{
					assertTrue(StringUtil.format("当前elementIdx:{},i：{}", elementIdx, i), head.next == chunk.subPages[i]);
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
	}
}
