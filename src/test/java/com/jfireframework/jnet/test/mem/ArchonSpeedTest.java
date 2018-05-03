package com.jfireframework.jnet.test.mem;

import java.lang.reflect.Field;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.baseutil.TRACEID;
import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.BatchRecycler;
import com.jfireframework.jnet.common.buffer.ChunkList;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

public class ArchonSpeedTest
{
	private final int			threadNum	= 1;
	private final int			count		= 100000;
	private static final Logger	logger		= LoggerFactory.getLogger(ArchonSpeedTest.class);
	
	/**
	 * 单线程请求释放
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void singleThreadBaseline() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		final Archon archon = Archon.heapPooledArchon(4, 1);
		PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
		Timewatch timewatch = new Timewatch();
		timewatch.start();
		for (long i = 0; i < count; i++)
		{
			archon.apply(buffer, 3);
			buffer.put((byte) 1);
			archon.recycle(buffer.chunk(), buffer.index());
		}
		timewatch.end();
		checkArchonState(archon);
		logger.debug("单线程释放:{}次耗时:{}毫秒", count, timewatch.getTotal());
		logger.debug("当前archon的统计情况：{}", archon.metric().toString());
	}
	
	private void checkArchonState(final Archon archon) throws NoSuchFieldException, IllegalAccessException
	{
		Field field = Archon.class.getDeclaredField("c25");
		field.setAccessible(true);
		ChunkList c25 = (ChunkList) field.get(archon);
		field = Archon.class.getDeclaredField("c50");
		field.setAccessible(true);
		ChunkList c50 = (ChunkList) field.get(archon);
		field = Archon.class.getDeclaredField("c75");
		field.setAccessible(true);
		ChunkList c75 = (ChunkList) field.get(archon);
		field = Archon.class.getDeclaredField("c100");
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
	
	@Test
	public void test2() throws InterruptedException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, BrokenBarrierException
	{
		final Archon archon = Archon.heapPooledArchon(4, 1);
		final CountDownLatch latch = new CountDownLatch(threadNum);
		final CyclicBarrier barrier = new CyclicBarrier(threadNum + 1);
		ExecutorService pool = Executors.newFixedThreadPool(threadNum);
		for (int k = 0; k < threadNum; k++)
		{
			pool.submit(new Runnable() {
				
				@Override
				public void run()
				{
					PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
					PooledIoBuffer buffer2 = PooledIoBuffer.heapBuffer();
					try
					{
						barrier.await();
						for (int i = 0; i < count; i++)
						{
							archon.apply(buffer, 1);
							archon.apply(buffer2, 3);
							archon.recycle(buffer2.chunk(), buffer2.index());
							archon.recycle(buffer.chunk(), buffer.index());
						}
						latch.countDown();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		barrier.await();
		long t0 = System.currentTimeMillis();
		latch.await();
		long t1 = System.currentTimeMillis();
		logger.debug("{}个线程单独申请单独释放{}次，使用同一个Archon，耗时{}毫秒", threadNum, count, (t1 - t0));
		// logger.debug("总计调用：\r\n{} ", ((PooledArchon) archon).statistics());
		checkArchonState(archon);
		logger.debug("当前archon的统计情况：{}", archon.metric().toString());
	}
	
	@Test
	// @Ignore
	public void test3() throws InterruptedException, BrokenBarrierException, NoSuchFieldException, IllegalAccessException
	{
		final Archon archon = Archon.heapPooledArchon(4, 1);
		final CountDownLatch latch = new CountDownLatch(threadNum);
		final CyclicBarrier barrier = new CyclicBarrier(threadNum + 1);
		ExecutorService pool = Executors.newFixedThreadPool(threadNum);
		final ExecutorService pool2 = Executors.newCachedThreadPool();
		final BatchRecycler batchRecycler = new BatchRecycler(pool2, archon);
		for (int k = 0; k < threadNum; k++)
		{
			pool.submit(new Runnable() {
				
				@Override
				public void run()
				{
					try
					{
						barrier.await();
						for (int i = 0; i < count; i++)
						{
							PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
							PooledIoBuffer buffer2 = PooledIoBuffer.heapBuffer();
							archon.apply(buffer, 1);
							archon.apply(buffer2, 3);
							batchRecycler.commit(buffer);
							batchRecycler.commit(buffer2);
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
		barrier.await();
		long t0 = System.currentTimeMillis();
		latch.await();
		pool2.shutdown();
		pool2.awaitTermination(10, TimeUnit.SECONDS);
		long t1 = System.currentTimeMillis();
		logger.debug("{}个线程单独申请1个线程批量释放{}次，使用同一个archon，耗时{}毫秒", threadNum, count, (t1 - t0));
		checkArchonState(archon);
		// logger.debug("总计调用：\r\n{} ", ((PooledArchon) archon).statistics());
		logger.debug("当前archon的统计情况：{}", archon.metric().toString());
	}
	
	/**
	 * 由单线程创建，在线程池A消耗，在线程池B收回。
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void test4() throws NoSuchMethodException, SecurityException, InterruptedException
	{
		String traceId = TRACEID.newTraceId();
		final Archon archon = Archon.heapPooledArchon(4, 1);
		ExecutorService pool = Executors.newFixedThreadPool(1);
		final ExecutorService pool2 = Executors.newFixedThreadPool(1);
		final CountDownLatch latch = new CountDownLatch(count);
		Timewatch timewatch = new Timewatch();
		for (int i = 0; i < count; i++)
		{
			final PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
			archon.apply(buffer, 3);
			pool.execute(new Runnable() {
				
				@Override
				public void run()
				{
					buffer.put((byte) 1);
					pool2.submit(new Runnable() {
						
						@Override
						public void run()
						{
							archon.recycle(buffer.chunk(), buffer.index());
							buffer.release();
							latch.countDown();
						}
					});
				}
			});
		}
		latch.await();
		timewatch.end();
		logger.debug("一个线程创建投递到第二个线程使用投递到第三个线程回收，共{}次， 消耗时间为:{}毫秒", count, timewatch.getTotal());
		logger.debug("当前archon的统计情况：{}", archon.metric().toString());
	}
}
