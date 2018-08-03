package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MemoryRegionCacheNormalTest
{
	PooledBufferAllocator	allocator	= new PooledBufferAllocator("test");
	private int				size;
	
	public MemoryRegionCacheNormalTest(int size)
	{
		this.size = size;
	}
	
	@Parameters
	public static List<Integer> params()
	{
		List<Integer> list = new LinkedList<>();
		int i = PooledBufferAllocator.PAGESIZE;
		while (i <= PooledBufferAllocator.MAX_CACHEED_BUFFER_CAPACITY)
		{
			list.add(i);
			i <<= 1;
		}
		return list;
	}
	
	@Test
	public void test() throws InterruptedException
	{
		test0(false, size);
		test0(true, size);
	}
	
	@SuppressWarnings("unchecked")
	private void test0(boolean preferDirect, int size) throws InterruptedException
	{
		int normalCacheSize = allocator.normalCacheSize;
		final Queue<IoBuffer> buffers = new LinkedList<>();
		Set<Chunk<?>> chunks = new HashSet<>();
		for (int i = 0; i < normalCacheSize; i++)
		{
			PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(size, preferDirect);
			buffers.add(buffer);
			chunks.add(buffer.chunk);
		}
		assertEquals(1, chunks.size());
		Chunk<?> chunk = chunks.iterator().next();
		int freeBytes = chunk.freeBytes;
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				while (buffers.isEmpty() == false)
				{
					buffers.poll().free();
				}
			}
		});
		thread.start();
		thread.join();
		assertEquals(freeBytes, chunk.freeBytes);
		ThreadCache threadCache = allocator.threadCache();
		@SuppressWarnings("rawtypes")
		MemoryRegionCache memoryRegionCache = threadCache.findCache(size, SizeType.NORMAL, threadCache.arena(preferDirect));
		assertEquals(normalCacheSize, memoryRegionCache.size());
		assertFalse(memoryRegionCache.offer(chunk, -1L));
		for (int i = 0; i < normalCacheSize; i++)
		{
			PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(size, preferDirect);
			buffers.add(buffer);
		}
		assertEquals("当前分配大小是:" + size, freeBytes, chunk.freeBytes);
		assertTrue(memoryRegionCache.isEmpty());
	}
}
