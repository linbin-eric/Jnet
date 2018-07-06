package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ArenaTest
{
	private BufferAllocator		allocator;
	private int					pagesize;
	private InstructmentParam	param;
	
	static class InstructmentParam
	{
		int		pageSize;
		int		maxLevel;
		int		numHeapArenas;
		int		numDirectArenas;
		int		maxCachedBufferCapacity;
		int		tinyCacheSize;
		int		smallCacheSize;
		int		normalCacheSize;
		boolean	useCacheForAllThread;
	}
	
	@Parameters
	public static Collection<?> data()
	{
		InstructmentParam param = new InstructmentParam();
		param.pageSize = PooledBufferAllocator.PAGESIZE;
		param.maxLevel = PooledBufferAllocator.MAXLEVEL;
		param.numHeapArenas = PooledBufferAllocator.NUM_HEAP_ARENA;
		param.numDirectArenas = PooledBufferAllocator.NUM_DIRECT_ARENA;
		param.maxCachedBufferCapacity = PooledBufferAllocator.MAX_CACHEED_BUFFER_CAPACITY;
		param.tinyCacheSize = 0;
		param.smallCacheSize = 0;
		param.normalCacheSize = 0;
		param.useCacheForAllThread = false;
		return Arrays.asList(param);
	}
	
	public ArenaTest(InstructmentParam instructmentParam)
	{
		allocator = new PooledBufferAllocator(instructmentParam.pageSize, instructmentParam.maxLevel, instructmentParam.numHeapArenas, //
		        instructmentParam.numDirectArenas, instructmentParam.maxCachedBufferCapacity, instructmentParam.tinyCacheSize, instructmentParam.smallCacheSize, instructmentParam.normalCacheSize, false);
		pagesize = instructmentParam.pageSize;
		param = instructmentParam;
	}
	
	/**
	 * 测试随着不同申请率，在多个ChunkList进行移动
	 */
	@Test
	public void test()
	{
		IoBuffer buffer = allocator.heapBuffer(pagesize);
		testMove(buffer);
		buffer = allocator.directBuffer(pagesize);
		testMove(buffer);
	}
	
	private void testMove(IoBuffer buffer)
	{
		Queue<IoBuffer> queue = new LinkedList<>();
		queue.add(buffer);
		ThreadCache threadCache = ((PooledBufferAllocator) allocator).threadCache();
		Arena<?> arena;
		if (buffer.isDirect())
		{
			arena = threadCache.directArena;
		}
		else
		{
			arena = threadCache.heapArena;
		}
		assertNull(arena.c100.head);
		assertNull(arena.c075.head);
		assertNull(arena.c050.head);
		assertNull(arena.c025.head);
		assertNull(arena.c000.head);
		assertNotNull(arena.cInt.head);
		Chunk<?> chunk = arena.cInt.head;
		assertEquals(1 << (param.maxLevel + 1), chunk.allocationCapacity.length);
		int total = 1 << param.maxLevel;
		int quarter = total >>> 2;
		for (int i = 1; i < quarter; i++)
		{
			allocate(pagesize, buffer, allocator, queue);
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNotNull(arena.cInt.head);
			assertTrue(chunk == arena.cInt.head);
		}
		assertEquals(25, chunk.usage());
		for (int i = 0; i < quarter; i++)
		{
			allocate(pagesize, buffer, allocator, queue);
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNotNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c000.head);
		}
		assertEquals(50, chunk.usage());
		for (int i = 0; i < quarter; i++)
		{
			allocate(pagesize, buffer, allocator, queue);
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNotNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c025.head);
		}
		assertEquals(75, chunk.usage());
		for (int i = 0; i < quarter - 1; i++)
		{
			allocate(pagesize, buffer, allocator, queue);
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNotNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c050.head);
		}
		assertEquals(99, chunk.usage());
		allocate(pagesize, buffer, allocator, queue);
		assertNotNull(arena.c100.head);
		assertNull(arena.c075.head);
		assertNull(arena.c050.head);
		assertNull(arena.c025.head);
		assertNull(arena.c000.head);
		assertNull(arena.cInt.head);
		assertTrue(chunk == arena.c100.head);
		assertEquals(100, chunk.usage());
		queue.poll().free();
		assertEquals(99, chunk.usage());
		assertNull(arena.c100.head);
		assertNotNull(arena.c075.head);
		assertNull(arena.c050.head);
		assertNull(arena.c025.head);
		assertNull(arena.c000.head);
		assertNull(arena.cInt.head);
		assertTrue(chunk == arena.c075.head);
		for (int i = 1; i < quarter; i++)
		{
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNotNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c075.head);
		}
		assertEquals(75, chunk.usage());
		int percent = (1 << param.maxLevel) / 100;
		for (int i = 0; i < percent; i++)
		{
			assertEquals(75, chunk.usage());
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNotNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c075.head);
		}
		for (int i = 0; i < quarter - percent; i++)
		{
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNotNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c050.head);
		}
		assertEquals(50, chunk.usage());
		for (int i = 0; i < percent; i++)
		{
			assertEquals(50, chunk.usage());
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNotNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c050.head);
		}
		for (int i = 0; i < quarter - percent; i++)
		{
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNotNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c025.head);
		}
		assertEquals(25, chunk.usage());
		for (int i = 0; i < percent; i++)
		{
			assertEquals(25, chunk.usage());
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNotNull(arena.c025.head);
			assertNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c025.head);
		}
		for (int i = 0; i < quarter - percent - 1; i++)
		{
			queue.poll().free();
			assertNull(arena.c100.head);
			assertNull(arena.c075.head);
			assertNull(arena.c050.head);
			assertNull(arena.c025.head);
			assertNotNull(arena.c000.head);
			assertNull(arena.cInt.head);
			assertTrue(chunk == arena.c000.head);
		}
		assertEquals(1, chunk.usage());
		queue.poll().free();
		assertTrue(queue.isEmpty());
		assertNull(arena.c100.head);
		assertNull(arena.c075.head);
		assertNull(arena.c050.head);
		assertNull(arena.c025.head);
		assertNull(arena.c000.head);
		assertNull(arena.cInt.head);
	}
	
	private void allocate(int size, IoBuffer predBuffer, BufferAllocator allocator, Queue<IoBuffer> queue)
	{
		if (predBuffer.isDirect())
		{
			queue.add(allocator.directBuffer(pagesize));
		}
		else
		{
			queue.add(allocator.heapBuffer(pagesize));
		}
	}
}
