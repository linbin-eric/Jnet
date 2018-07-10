package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BufferRecycleTest
{
	PooledUnThreadCacheBufferAllocator allocator = new PooledUnThreadCacheBufferAllocator();
	
	@Test
	public void test()
	{
		IoBuffer buffer = allocator.ioBuffer(12);
		buffer.free();
		IoBuffer buffer2 = allocator.ioBuffer(5689);
		assertTrue(buffer == buffer2);
		buffer2.free();
	}
	
	@Test
	public void test2() throws InterruptedException
	{
		final IoBuffer buffer = allocator.ioBuffer(12);
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				buffer.free();
			}
		});
		thread.start();
		thread.join();
		IoBuffer buffer2 = allocator.ioBuffer(2);
		assertTrue(buffer2 == buffer);
		buffer2.free();
	}
}
