package com.jfireframework.jnet;

import com.jfireframework.jnet.client.DefaultClient;
import com.jfireframework.jnet.client.JnetClient;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledBufferAllocator;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.processor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.processor.ThreadAttachProcessor;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.server.AioServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * 基本测试。用于验证代码的正确性
 *
 * @author linbin
 */
@RunWith(Parameterized.class)
public class BaseTest
{
    private static final Logger          logger       = LoggerFactory.getLogger(BaseTest.class);
    private              AioServer       aioServer;
    private              String          ip           = "127.0.0.1";
    private              int             port         = 7598;
    private              int             numPerThread = 4000000;
    private              int             numClients   = 4;
    private              JnetClient[]    clients;
    private              CountDownLatch  latch        = new CountDownLatch(numClients);
    private              int[][]         results;
    private              BufferAllocator bufferAllocator;

    @Parameters(name = "IO模式:{2}")
    public static Collection<Object[]> params()
    {
        return Arrays.asList(new Object[][]{ //
//                {PooledUnRecycleBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.IO}, //
                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.IO}, //
                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.Channel}, //
                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.THREAD}, //
//                {PooledUnThreadCacheBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.IO}, //
//                {UnPooledRecycledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.IO}, //
//                {UnPooledUnRecycledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.IO}, //
//                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.Channel}, //
//                {PooledBufferAllocator.DEFAULT, 1024 * 1024 * 8, IoMode.THREAD}, //
        });
    }

    public BaseTest(final BufferAllocator bufferAllocator, int batchWriteNum, final IoMode ioMode)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setAllocator(bufferAllocator);
        channelConfig.setMaxBatchWrite(batchWriteNum);
        this.bufferAllocator = bufferAllocator;
        clients = new JnetClient[numClients];
        results = new int[numClients][numPerThread];
        for (int i = 0; i < numClients; i++)
        {
            results[i] = new int[numPerThread];
            Arrays.fill(results[i], -1);
        }
        final ExecutorService fixService = Executors.newCachedThreadPool(new ThreadFactory()
        {
            int count = 0;

            @Override
            public Thread newThread(Runnable r)
            {
                return new FastThreadLocalThread(r, "business-worker-" + (count++));
            }
        });
        ChannelContextInitializer initializer = new ChannelContextInitializer()
        {

            @Override
            public void onChannelContextInit(final ChannelContext channelContext)
            {
                switch (ioMode)
                {
                    case IO:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024, bufferAllocator), //
                                new BindDownAndUpStreamDataProcessor<IoBuffer>()
                                {
                                    @Override
                                    public void process(IoBuffer data) throws Throwable
                                    {
                                        data.addReadPosi(-4);
                                        downStream.process(data);
                                    }
                                });
                        break;
                    case Channel:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024, bufferAllocator), //
                                new ChannelAttachProcessor(fixService), //
                                new BindDownAndUpStreamDataProcessor<IoBuffer>()
                                {

                                    @Override
                                    public void process(IoBuffer data) throws Throwable
                                    {
                                        data.addReadPosi(-4);
                                        downStream.process(data);
                                    }
                                });
                        break;
                    case THREAD:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024, bufferAllocator), //
                                new ThreadAttachProcessor(fixService), //
                                new BindDownAndUpStreamDataProcessor<IoBuffer>()
                                {

                                    @Override
                                    public void process(IoBuffer data) throws Throwable
                                    {
                                        data.addReadPosi(-4);
                                        downStream.process(data);
                                    }
                                });
                        break;
                    default:
                        break;
                }
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
            ChannelContextInitializer childIniter = new ChannelContextInitializer()
            {

                @Override
                public void onChannelContextInit(ChannelContext channelContext)
                {
                    channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 4, bufferAllocator), //
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
                                public void process(IoBuffer buffer) throws Throwable
                                {
                                    int j = buffer.getInt();
                                    result[j] = j;
                                    buffer.free();
                                    count++;
                                    if (count == numPerThread)
                                    {
                                        latch.countDown();
                                    }
                                }
                            });
                }
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
        IO, Channel, THREAD
    }
}
