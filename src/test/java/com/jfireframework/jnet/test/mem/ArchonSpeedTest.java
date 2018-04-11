package com.jfireframework.jnet.test.mem;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.HeapPooledArchon;
import com.jfireframework.jnet.common.mem.archon.PooledArchon;
import com.jfireframework.jnet.common.mem.chunk.ChunkList;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class ArchonSpeedTest
{
	@Test
	public void test() throws InterruptedException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		final Archon archon = new HeapPooledArchon(4, 1);
		final int count = 3000000;
		int threadNum = 30;
		final CountDownLatch latch = new CountDownLatch(threadNum);
		final CyclicBarrier barrier = new CyclicBarrier(threadNum);
		ExecutorService pool = Executors.newFixedThreadPool(threadNum);
		long t0 = System.currentTimeMillis();
		for (int k = 0; k < threadNum; k++)
		{
			pool.submit(new Runnable() {
				
				@Override
				public void run()
				{
					HeapIoBuffer handler = new HeapIoBuffer();
					try
					{
						barrier.await();
						for (int i = 0; i < count; i++)
						{
							archon.apply(1, handler);
							archon.recycle(handler);
						}
						latch.countDown();
					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
		latch.await();
		long t1 = System.currentTimeMillis();
		System.out.println((t1 - t0));
		Field field = PooledArchon.class.getDeclaredField("c25");
		field.setAccessible(true);
		ChunkList c25 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c50");
		field.setAccessible(true);
		ChunkList c50 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c75");
		field.setAccessible(true);
		ChunkList c75 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c100");
		field.setAccessible(true);
		ChunkList c100 = (ChunkList) field.get(archon);
		if (c25.head() != null)
		{
			System.out.println("c25:" + c25.head().usage());
			Assert.fail();
		}
		if (c50.head() != null)
		{
			System.out.println("c50:" + c50.head().usage());
			Assert.fail();
		}
		if (c75.head() != null)
		{
			System.out.println("c75:" + c75.head().usage());
			Assert.fail();
		}
		if (c100.head() != null)
		{
			System.out.println("c100:" + c100.head().usage());
			Assert.fail();
		}
	}
}
