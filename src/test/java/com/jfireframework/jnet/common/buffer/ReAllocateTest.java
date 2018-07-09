package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

public class ReAllocateTest
{
	PooledUnThreadCacheBufferAllocator allocator = new PooledUnThreadCacheBufferAllocator();
	
	@Test
	public void test()
	{
		test0(true);
		test0(false);
	}
	
	private void test0(boolean preferDirect)
	{
		PooledBuffer<?> buffer = (PooledBuffer<?>) allocator.ioBuffer(16, preferDirect);
		int offset = buffer.offset;
		long handle = buffer.handle;
		assertEquals(16, buffer.capacity());
		buffer.putInt(8);
		buffer.putInt(8);
		buffer.putInt(8);
		buffer.putInt(8);
		assertEquals(16, buffer.capacity());
		buffer.putInt(8);
		assertEquals(32, buffer.capacity());
		assertEquals(20, buffer.getWritePosi());
		assertEquals(0, buffer.getReadPosi());
		assertNotEquals(offset, buffer.offset);
		assertNotEquals(handle, buffer.handle);
		for (int i = 0; i < 5; i++)
		{
			assertEquals(8, buffer.getInt());
		}
		assertEquals(0, buffer.remainRead());
	}
}
