package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertEquals;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

public class NormalAllocateTest
{
	int						pagesize	= 1024;
	PooledBufferAllocator	allocator	= new PooledBufferAllocator(pagesize, 4, 1, 1, 0, 0, 0, 0, false, true);
	
	@Test
	public void test()
	{
		test0(true);
		test0(false);
	}
	
	private void test0(boolean direct)
	{
		List<IoBuffer> buffers = new LinkedList<>();
		for (int i = 0; i < 16; i++)
		{
			PooledBuffer<?> ioBuffer = (PooledBuffer<?>) allocator.ioBuffer(pagesize, direct);
			long handle = ioBuffer.handle;
			assertEquals(16 + i, handle);
			buffers.add(ioBuffer);
		}
		for (int i = 0; i < 8; i++)
		{
			PooledBuffer<?> ioBuffer = (PooledBuffer<?>) allocator.ioBuffer(pagesize << 1, direct);
			long handle = ioBuffer.handle;
			assertEquals(8 + i, handle);
			buffers.add(ioBuffer);
		}
		for (int i = 0; i < 4; i++)
		{
			PooledBuffer<?> ioBuffer = (PooledBuffer<?>) allocator.ioBuffer(pagesize << 2, direct);
			long handle = ioBuffer.handle;
			assertEquals(4 + i, handle);
			buffers.add(ioBuffer);
		}
		for (int i = 0; i < 2; i++)
		{
			PooledBuffer<?> ioBuffer = (PooledBuffer<?>) allocator.ioBuffer(pagesize << 3, direct);
			long handle = ioBuffer.handle;
			assertEquals(2 + i, handle);
			buffers.add(ioBuffer);
		}
		for (int i = 0; i < 1; i++)
		{
			PooledBuffer<?> ioBuffer = (PooledBuffer<?>) allocator.ioBuffer(pagesize << 4, direct);
			long handle = ioBuffer.handle;
			assertEquals(1 + i, handle);
			buffers.add(ioBuffer);
		}
	}
}
