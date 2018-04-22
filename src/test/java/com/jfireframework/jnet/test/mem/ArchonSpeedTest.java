package com.jfireframework.jnet.test.mem;

import java.lang.reflect.Field;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.BatchRecycler;
import com.jfireframework.jnet.common.buffer.ChunkList;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledArchon;

public class ArchonSpeedTest
{
    private final int           count  = 100000;
    private static final Logger logger = LoggerFactory.getLogger(ArchonSpeedTest.class);
    
    /**
     * 单线程请求释放
     * 
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public void test1() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Archon archon = PooledArchon.heapPooledArchon(4, 1);
        IoBuffer buffer = IoBuffer.heapIoBuffer();
        IoBuffer buffer2 = IoBuffer.heapIoBuffer();
        Timewatch timewatch = new Timewatch();
        timewatch.start();
        for (int i = 0; i < count; i++)
        {
            archon.apply(1, buffer);
            archon.apply(3, buffer2);
            archon.recycle(buffer2);
            archon.recycle(buffer);
        }
        timewatch.end();
        checkArchonState(archon);
        logger.debug("单线程释放:{}次耗时:{}毫秒", count, timewatch.getTotal());
    }
    
    private void checkArchonState(final Archon archon) throws NoSuchFieldException, IllegalAccessException
    {
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
    
    @Test
    public void test2() throws InterruptedException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, BrokenBarrierException
    {
        final Archon archon = PooledArchon.heapPooledArchon(4, 1);
        int threadNum = 4;
        final CountDownLatch latch = new CountDownLatch(threadNum);
        final CyclicBarrier barrier = new CyclicBarrier(threadNum + 1);
        ExecutorService pool = Executors.newFixedThreadPool(threadNum);
        for (int k = 0; k < threadNum; k++)
        {
            pool.submit(new Runnable() {
                
                @Override
                public void run()
                {
                    IoBuffer buffer = IoBuffer.heapIoBuffer();
                    IoBuffer buffer2 = IoBuffer.heapIoBuffer();
                    try
                    {
                        barrier.await();
                        for (int i = 0; i < count; i++)
                        {
                            archon.apply(1, buffer);
                            archon.apply(3, buffer2);
                            archon.recycle(buffer2);
                            archon.recycle(buffer);
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
        long t1 = System.currentTimeMillis();
        logger.debug("{}个线程单独申请单独释放{}次耗时{}毫秒", threadNum, count, (t1 - t0));
        checkArchonState(archon);
    }
    
    @Test
    public void test3() throws InterruptedException, BrokenBarrierException, NoSuchFieldException, IllegalAccessException
    {
        final Archon archon = PooledArchon.heapPooledArchon(4, 1);
        int threadNum = 4;
        final CountDownLatch latch = new CountDownLatch(threadNum);
        final CyclicBarrier barrier = new CyclicBarrier(threadNum + 1);
        ExecutorService pool = Executors.newFixedThreadPool(threadNum);
        ExecutorService pool2 = Executors.newFixedThreadPool(1);
        final BatchRecycler batchRecycler = new BatchRecycler(pool2, archon);
        for (int k = 0; k < threadNum; k++)
        {
            pool.submit(new Runnable() {
                
                @Override
                public void run()
                {
                    IoBuffer buffer = IoBuffer.heapIoBuffer();
                    IoBuffer buffer2 = IoBuffer.heapIoBuffer();
                    try
                    {
                        barrier.await();
                        for (int i = 0; i < count; i++)
                        {
                            archon.apply(1, buffer);
                            archon.apply(3, buffer2);
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
        long t1 = System.currentTimeMillis();
        logger.debug("{}个线程单独申请批量释放{}次耗时{}毫秒", threadNum, count, (t1 - t0));
        checkArchonState(archon);
    }
}
