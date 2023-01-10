package com.jfirer.jnet;

import com.jfirer.jnet.client.DefaultClient;
import com.jfirer.jnet.client.JnetClient;
import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;
import com.jfirer.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 基本测试。用于验证代码的正确性
 *
 * @author linbin
 */
public class BaseTest
{
    private static final Logger          logger       = LoggerFactory.getLogger(BaseTest.class);
    private              AioServer       aioServer;
    private              String          ip           = "127.0.0.1";
    private              int             port         = 7598;
    private              int             numPerThread = 3;
    private              int             numClients   = 1;
    private              JnetClient[]    clients;
    private              CountDownLatch  latch        = new CountDownLatch(numClients);
    private              int[][]         results;
    private              BufferAllocator bufferAllocator;
    AtomicInteger count = new AtomicInteger(0);

    public BaseTest()
    {
        ChannelConfig channelConfig = new ChannelConfig();
        this.bufferAllocator = channelConfig.getAllocator();
        clients = new JnetClient[numClients];
        results = new int[numClients][numPerThread];
        for (int i = 0; i < numClients; i++)
        {
            results[i] = new int[numPerThread];
            Arrays.fill(results[i], -1);
        }
        ChannelContextInitializer initializer = new ChannelContextInitializer()
        {
            @Override
            public void onChannelContextInit(final ChannelContext channelContext)
            {
                Pipeline pipeline = channelContext.pipeline();
                pipeline.addReadProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024, bufferAllocator));
                pipeline.addReadProcessor((ReadProcessor) (data, ctx) -> {
                    System.out.println("shoudao");
                    count.incrementAndGet();
                    ((IoBuffer) data).addReadPosi(-4);
                    pipeline.fireWrite(data);
                });
            }
        };
        channelConfig.setIp(ip);
        channelConfig.setPort(port);
        aioServer = AioServer.newAioServer(channelConfig, initializer);
        aioServer.start();
        for (int i = 0; i < numClients; i++)
        {
            final int   index  = i;
            final int[] result = results[index];
            ChannelContextInitializer childIniter = channelContext -> {
                Pipeline pipeline = channelContext.pipeline();
                pipeline.addReadProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 4, bufferAllocator));
                pipeline.addReadProcessor(new ReadProcessor()
                {
                    int count = 0;

                    @Override
                    public void read(Object data, ReadProcessorNode ctx)
                    {
                        System.out.println("shoudao 2");
                        try
                        {
                            IoBuffer buffer = (IoBuffer) data;
                            int      j      = buffer.getInt();
                            result[j] = j;
                            buffer.free();
                            count++;
                            if (count == numPerThread)
                            {
                                latch.countDown();
                            }
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            };
            clients[i] = new DefaultClient(channelConfig, childIniter);
        }
    }

    @Test
    public void test() throws InterruptedException
    {
        final CyclicBarrier  barrier = new CyclicBarrier(numClients);
        final CountDownLatch finish  = new CountDownLatch(numClients);
        for (int i = 0; i < numClients; i++)
        {
            final int index = i;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    JnetClient client = clients[index];
                    try
                    {
                        barrier.await();
                    }
                    catch (InterruptedException | BrokenBarrierException e1)
                    {
                        e1.printStackTrace();
                    }
                    int batch = 1000;
                    for (int j = 0; j < numPerThread; )
                    {
                        IoBuffer buffer = bufferAllocator.ioBuffer(8);
                        int      num    = j;
                        int      max    = num + batch > numPerThread ? numPerThread : num + batch;
                        for (; num < max; num++)
                        {
                            System.out.println("写出");
                            buffer.putInt(8);
                            buffer.putInt(num);
                        }
                        j = num;
                        try
                        {
                            client.write(buffer);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            ;
                        }
                    }
                    finish.countDown();
                }
            }).start();
        }
        try
        {
            finish.await();
            logger.debug("写出完毕");
            latch.await(10000, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        for (int index = 0; index < numClients; index++)
        {
            int[] result = results[index];
            for (int i = 0; i < numPerThread; i++)
            {
                assertEquals("序号" + index, i, result[i]);
            }
        }
        System.out.println("验证通过");
        for (JnetClient each : clients)
        {
            each.close();
        }
        logger.info("测试完毕");
        aioServer.termination();
    }

    static enum IoMode
    {
        IO,
        Channel,
        THREAD
    }
}
