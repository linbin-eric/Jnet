package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HugeAllocateTest
{
	PooledBufferAllocator allocator = PooledBufferAllocator.DEFAULT;
	
	@Test
	public void testHeap()
	{
		ThreadCache threadCache = allocator.threadCache();
		Arena<?> arena = threadCache.heapArena;
		int allocateCapacity = arena.chunkSize + 1;
		PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.heapBuffer(allocateCapacity);
		test0(allocateCapacity, buffer, arena);
	}
	
	private void test0(int allocateCapacity, PooledBuffer<?> buffer, Arena<?> arena)
	{
		assertTrue(buffer.chunk.unpooled);
		assertEquals(0, buffer.offset);
		assertEquals(allocateCapacity, buffer.capacity);
		assertNull(arena.cInt.head);
		assertNull(arena.c000.head);
		assertNull(arena.c025.head);
		assertNull(arena.c050.head);
		assertNull(arena.c075.head);
		assertNull(arena.c100.head);
		buffer.free();
		assertNull(arena.cInt.head);
		assertNull(arena.c000.head);
		assertNull(arena.c025.head);
		assertNull(arena.c050.head);
		assertNull(arena.c075.head);
		assertNull(arena.c100.head);
	}
	
	@Test
	public void testDirect()
	{
		ThreadCache threadCache = allocator.threadCache();
		Arena<?> arena = threadCache.directArena;
		int allocateCapacity = arena.chunkSize + 1;
		PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.directBuffer(allocateCapacity);
		test0(allocateCapacity, buffer, arena);
	}
}
