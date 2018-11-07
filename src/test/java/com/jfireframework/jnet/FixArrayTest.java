package com.jfireframework.jnet;

import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.common.util.FixArray;
import com.jfireframework.jnet.common.util.MPSCFixArray;
import org.junit.Test;

import java.util.concurrent.CyclicBarrier;

public class FixArrayTest
{
    final int    sendNum = 1000000;
    final String value   = "";
    int capacity          = 2048;
    int producerThreadNum = 24;
    final int    total   = sendNum * producerThreadNum;

    public void testBastUtilArrayQueue() throws InterruptedException
    {
        final FixArray<Slot> baseUtilArrayQueue = new MPSCFixArray<Slot>(capacity)
        {
            @Override
            protected Slot newInstance()
            {
                return new Slot();
            }
        };
        final CyclicBarrier barrier = new CyclicBarrier(producerThreadNum + 1);
        for (int i = 0; i < producerThreadNum; i++)
        {
            new Thread(new Runnable()
            {

                @Override
                public void run()
                {
                    try
                    {
                        barrier.await();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < sendNum; i++)
                    {
                        long index = baseUtilArrayQueue.nextOfferIndex();
                        if (index == -1)
                        {
                            while ((index = baseUtilArrayQueue.nextOfferIndex()) == -1)
                            {
                                Thread.yield();
                            }
                        }
                        Slot slot = baseUtilArrayQueue.getSlot(index);
                        slot.value = value;
                        baseUtilArrayQueue.commit(index);
                    }
                }
            }).start();
        }
        Thread thread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    barrier.await();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                Timewatch timewatch = new Timewatch();
                timewatch.start();
                for (int i = 0; i < total; i++)
                {
                    long avail = baseUtilArrayQueue.nextAvail();
                    if (avail == -1)
                    {
                        while ((avail = baseUtilArrayQueue.nextAvail()) == -1)
                        {
                            Thread.yield();
                        }
                    }
                    // Slot slot = baseUtilArrayQueue.getSlot(avail);
                    // slot.value = null;
                    baseUtilArrayQueue.comsumeAvail(avail);
                }
                timewatch.end();
                System.out.println("消费" + total / 10000 + "w个数据耗时:" + timewatch.getTotal() + "毫秒");
                if (baseUtilArrayQueue.isEmpty() == false)
                {
                    System.err.println("异常");
                }
            }
        });
        thread.start();
        thread.join();
    }

    @Test
    public void test() throws InterruptedException
    {
        testBastUtilArrayQueue();
    }

    class Slot
    {
        String value;
    }
}
