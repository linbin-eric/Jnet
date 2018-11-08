package com.jfireframework.jnet;

import com.jfireframework.jnet.client.JnetClient;
import com.jfireframework.jnet.client.JnetClientBuilder;
import com.jfireframework.jnet.common.api.BackPressureMode;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledBufferAllocator;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.internal.DefaultAcceptHandler;
import com.jfireframework.jnet.common.processor.BackPressureHelper;
import com.jfireframework.jnet.common.processor.ChannelAttachProcessor;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.AioServerBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 基本测试。用于验证代码的正确性
 *
 * @author linbin
 */
@RunWith(Parameterized.class)
public class BaseTest
{
    private static final Logger         logger       = LoggerFactory.getLogger(BaseTest.class);
    private              AioServer      aioServer;
    private              String         ip           = "127.0.0.1";
    private              int            port         = 7598;
    private              int            numPerThread = 100000;
    private              int            numClients   = 10;
    private              JnetClient[]   clients;
    private              CountDownLatch latch        = new CountDownLatch(numClients);
    private              int[][]        results;

    public BaseTest(final BufferAllocator bufferAllocator, int batchWriteNum, final BackPressureMode backPressureMode, final IoMode ioMode)
    {
        clients = new JnetClient[numClients];
        results = new int[numClients][numPerThread];
        for (int i = 0; i < numClients; i++)
        {
            results[i] = new int[numPerThread];
            Arrays.fill(results[i], -1);
        }
        AioServerBuilder builder = new AioServerBuilder();
        final ExecutorService fixService = Executors.newFixedThreadPool(4, new ThreadFactory()
        {
            AtomicInteger atomicInteger = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                int count = atomicInteger.getAndIncrement();
                return new Thread(r, "channelWorker-" + (count));
            }
        });
        builder.setAcceptHandler(new DefaultAcceptHandler(null, bufferAllocator, batchWriteNum, new ChannelContextInitializer()
        {

            @Override
            public void onChannelContextInit(final ChannelContext channelContext)
            {
                switch (ioMode)
                {
                    case IO:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 100, bufferAllocator), //
                                new BindDownAndUpStreamDataProcessor<IoBuffer>()
                                {

                                    @Override
                                    public boolean process(IoBuffer data) throws Throwable
                                    {
                                        data.addReadPosi(-4);
                                        return downStream.process(data);
                                    }
                                }, new BackPressureHelper());
                        break;
                    case Channel:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 100, bufferAllocator), //
                               new BackPressureHelper(),//
                                new ChannelAttachProcessor(fixService), //
                                new BindDownAndUpStreamDataProcessor<IoBuffer>()
                                {

                                    @Override
                                    public boolean process(IoBuffer data) throws Throwable
                                    {
                                        data.addReadPosi(-4);
                                        return downStream.process(data);
                                    }
                                }, new BackPressureHelper());
                        break;
                    default:
                        break;
                }
            }
        }, backPressureMode));
        builder.setBindIp(ip);
        builder.setPort(port);
        aioServer = builder.build();
        aioServer.start();
        for (int i = 0; i < numClients; i++)
        {
            final int         index             = i;
            final int[]       result            = results[index];
            JnetClientBuilder jnetClientBuilder = new JnetClientBuilder();
            jnetClientBuilder.setServerIp(ip);
            jnetClientBuilder.setPort(port);
            jnetClientBuilder.setBackPressureMode(new BackPressureMode());
            jnetClientBuilder.setChannelContextInitializer(new ChannelContextInitializer()
            {

                @Override
                public void onChannelContextInit(ChannelContext channelContext)
                {
                    channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1000, bufferAllocator), //
                            new DataProcessor<IoBuffer>()
                            {
                                int count = 0;

                                @Override
                                public void bind(ChannelContext channelContext)
                                {
                                    ;
                                }

                                @Override
                                public void bindDownStream(DataProcessor<?> downStream)
                                {
                                }

                                @Override
                                public void bindUpStream(DataProcessor<?> upStream)
                                {
                                }

                                @Override
                                public boolean process(IoBuffer buffer) throws Throwable
                                {
                                    int j = buffer.getInt();
                                    result[j] = j;
                                    buffer.free();
                                    count++;
                                    if (count == numPerThread)
                                    {
                                        latch.countDown();
                                    }
                                    return true;
                                }

                                @Override
                                public boolean canAccept()
                                {
                                    return false;
                                }

                                @Override
                                public void notifyedWriteAvailable() throws Throwable
                                {
                                }
                            });
                }
            });
            jnetClientBuilder.setAllocator(bufferAllocator);
            clients[i] = jnetClientBuilder.build();
        }
    }

    @Parameters(name = "IO模式:{3}，是否开启背压:{2}")
    public static Collection<Object[]> params()
    {
        return Arrays.asList(new Object[][]{ //
//                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 2, new BackPressureMode(), IoMode.IO}, //
//                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 2, new BackPressureMode(1024), IoMode.IO}, //
                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 2, new BackPressureMode(), IoMode.Channel}, //
        });
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
                    } catch (InterruptedException | BrokenBarrierException e1)
                    {
                        e1.printStackTrace();
                    }
                    for (int j = 0; j < numPerThread; j++)
                    {
                        IoBuffer buffer = PooledBufferAllocator.DEFAULT.ioBuffer(8);
                        buffer.putInt(8);
                        buffer.putInt(j);
                        try
                        {
                            client.write(buffer);
                        } catch (Exception e)
                        {
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
        } catch (InterruptedException e)
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
        for (JnetClient each : clients)
        {
            each.close();
        }
        logger.info("测试完毕");
        aioServer.termination();
    }

    static enum IoMode
    {
        IO, Channel
    }
}
