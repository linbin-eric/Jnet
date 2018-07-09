package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Test;

public class ChunkListTest
{
	PooledUnThreadCacheBufferAllocator allocator = new PooledUnThreadCacheBufferAllocator();
	
	@Test
	public void test0()
	{
		test0(true);
		test0(false);
	}
	
	private void test0(boolean preferDirect)
	{
		int chunkSize = allocator.pagesize << allocator.maxLevel;
		int size = chunkSize >> 2;
		Queue<IoBuffer> buffers = new LinkedList<>();
		for (int i = 0; i < 4; i++)
		{
			IoBuffer buffer = allocator.ioBuffer(size, preferDirect);
			if (i != 3)
			{
				buffers.add(buffer);
			}
		}
		Arena<?> arena = allocator.threadCache().arena(preferDirect);
		Chunk<?> chunk1 = arena.c100.head;
		for (int i = 0; i < 4; i++)
		{
			IoBuffer buffer = allocator.ioBuffer(size, preferDirect);
			if (i != 3)
			{
				buffers.add(buffer);
			}
		}
		Chunk<?> chunk2 = arena.c100.head;
		assertTrue(chunk1 != chunk2);
		while (buffers.isEmpty() == false)
		{
			buffers.poll().free();
		}
		assertTrue(chunk2.next == chunk1);
		allocator.ioBuffer(size, preferDirect);
		allocator.ioBuffer(size, preferDirect);
		assertEquals(75, chunk2.usage());
		allocator.ioBuffer(size << 1, preferDirect);
		assertEquals(75, chunk1.usage());
		allocator.ioBuffer(size << 1, preferDirect);
		Chunk<?> chunk3 = arena.c000.head;
		assertNotNull(chunk3);
		assertEquals(50, chunk3.usage());
	}
}
