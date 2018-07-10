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
		buffer.putInt(4);
		buffer.putInt(5);
		buffer.putInt(6);
		buffer.putInt(7);
		assertEquals(16, buffer.capacity());
		buffer.putInt(8);
		assertEquals(32, buffer.capacity());
		assertEquals(20, buffer.getWritePosi());
		assertEquals(0, buffer.getReadPosi());
		assertNotEquals(offset, buffer.offset);
		assertNotEquals(handle, buffer.handle);
		assertEquals(4, buffer.getInt());
		assertEquals(5, buffer.getInt());
		assertEquals(6, buffer.getInt());
		assertEquals(7, buffer.getInt());
		assertEquals(8, buffer.getInt());
		assertEquals(0, buffer.remainRead());
	}
}
