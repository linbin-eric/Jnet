package com.jfireframework.jnet.test.server;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.baseutil.StringUtil;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.HeapByteBuf;
import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.client.build.AioClientBuilder;
import com.jfireframework.jnet.client.client.AioClient;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.bufstorage.impl.MpscBufStorage;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.common.decodec.impl.TotalLengthFieldBasedFrameDecoderByHeap;
import com.jfireframework.jnet.common.streamprocessor.LengthEncodeProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorIndexFlag;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.build.AioServerBuilder;

@RunWith(Parameterized.class)
public class SpeedTest
{
    private int    port            = 8546;
    private int    clientThreadNum = 100;
    private int    sendCount       = 5000;
    private int    sum             = clientThreadNum * sendCount;
    private IoMode serverMode;
    private IoMode clientMode;
    
    public SpeedTest(IoMode serverMode, IoMode clientMode)
    {
        this.serverMode = serverMode;
        this.clientMode = clientMode;
    }
    
    @Parameters
    public static Collection<IoMode[]> data()
    {
        return Arrays.asList(new IoMode[][] { //
                { IoMode.SIMPLE, IoMode.SIMPLE }, //
                { IoMode.SIMPLE, IoMode.THREAD_ATTACH }, //
                { IoMode.SIMPLE, IoMode.CHANNEL_ATTACH }, //
                { IoMode.SIMPLE, IoMode.MUTLI_ATTACH }, //
                { IoMode.THREAD_ATTACH, IoMode.SIMPLE }, //
                { IoMode.THREAD_ATTACH, IoMode.THREAD_ATTACH }, //
                { IoMode.THREAD_ATTACH, IoMode.CHANNEL_ATTACH }, //
                { IoMode.THREAD_ATTACH, IoMode.MUTLI_ATTACH }, //
                { IoMode.CHANNEL_ATTACH, IoMode.SIMPLE }, //
                { IoMode.CHANNEL_ATTACH, IoMode.THREAD_ATTACH }, //
                { IoMode.CHANNEL_ATTACH, IoMode.CHANNEL_ATTACH }, //
                { IoMode.CHANNEL_ATTACH, IoMode.MUTLI_ATTACH }, //
                { IoMode.MUTLI_ATTACH, IoMode.SIMPLE }, //
                { IoMode.MUTLI_ATTACH, IoMode.THREAD_ATTACH }, //
                { IoMode.MUTLI_ATTACH, IoMode.CHANNEL_ATTACH }, //
                { IoMode.MUTLI_ATTACH, IoMode.MUTLI_ATTACH }, //
        });
    }
    
    private AioServer buildServer()
    {
        AioServerBuilder serverBuilder = new AioServerBuilder();
        serverBuilder.setBindIp("127.0.0.1");
        serverBuilder.setPort(port);
        serverBuilder.setIoMode(serverMode);
        serverBuilder.setIoProcessorNum(8);
        serverBuilder.setBusinessProcessorNum(8);
        serverBuilder.setChannelContextBuilder(new ChannelContextBuilder() {
            
            @Override
            public ChannelContextConfig onConnect(AsynchronousSocketChannel socketChannel)
            {
                ChannelContextConfig config = new ChannelContextConfig();
                config.setBufStorage(new MpscBufStorage());
                config.setFrameDecodec(new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500));
                config.setMaxMerge(1000);
                config.setInProcessors(new StreamProcessor() {
                    
                    @Override
                    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
                    {
                        ByteBuf<?> buf = (ByteBuf<?>) data;
                        buf.readIndex(0);
                        return buf;
                    }
                });
                return config;
            }
            
            @Override
            public void afterContextBuild(ChannelContext serverChannelContext)
            {
                // TODO Auto-generated method stub
                
            }
        });
        AioServer aioServer = serverBuilder.build();
        return aioServer;
    }
    
    private AioClientBuilder buildClient(final AtomicInteger total, final CountDownLatch latch)
    {
        AioClientBuilder clientBuilder = new AioClientBuilder();
        clientBuilder.setServerIp("127.0.0.1");
        clientBuilder.setBusinessThreadNum(8);
        clientBuilder.setIoThreadNum(8);
        clientBuilder.setPort(port);
        clientBuilder.setIoMode(clientMode);
        clientBuilder.setChannelContextBuilder(new ChannelContextBuilder() {
            
            @Override
            public ChannelContextConfig onConnect(AsynchronousSocketChannel socketChannel)
            {
                ChannelContextConfig config = new ChannelContextConfig();
                config.setFrameDecodec(new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500));
                config.setMaxMerge(1000);
                config.setBufStorage(new MpscBufStorage());
                config.setInProcessors(new StreamProcessor() {
                    
                    @Override
                    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
                    {
                        ByteBuf<?> buf = (ByteBuf<?>) data;
                        buf.release();
                        int now = total.incrementAndGet();
                        if (now == sum)
                        {
                            latch.countDown();
                        }
                        return null;
                    }
                });
                config.setOutProcessors(new StreamProcessor() {
                    
                    @Override
                    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
                    {
                        ByteBuf<?> buf = HeapByteBuf.allocate(28);
                        buf.addWriteIndex(4);
                        buf.put((byte[]) data);
                        return buf;
                    }
                }, new LengthEncodeProcessor(0, 4));
                return config;
            }
            
            @Override
            public void afterContextBuild(ChannelContext clientChannelContext)
            {
                // TODO Auto-generated method stub
                
            }
        });
        return clientBuilder;
    }
    
    @Test
    public void test() throws Throwable
    {
        for (int i = 0; i < 5; i++)
        {
            dotest();
        }
    }
    
    public void dotest() throws Throwable
    {
        AtomicInteger total = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        final byte[] content = "hello world".getBytes("utf8");
        AioServer aioServer = buildServer();
        aioServer.start();
        final CyclicBarrier barrier = new CyclicBarrier(clientThreadNum + 1);
        ExecutorService executorService = Executors.newFixedThreadPool(clientThreadNum);
        final AioClientBuilder clientBuilder = buildClient(total, latch);
        for (int i = 0; i < clientThreadNum; i++)
        {
            executorService.execute(new Runnable() {
                
                @Override
                public void run()
                {
                    final AioClient client = clientBuilder.build();
                    client.connect();
                    try
                    {
                        barrier.await();
                    }
                    catch (InterruptedException | BrokenBarrierException e1)
                    {
                        e1.printStackTrace();
                    }
                    for (int i = 0; i < sendCount; i++)
                    {
                        try
                        {
                            client.write(content);
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        Timewatch timewatch = new Timewatch();
        barrier.await();
        timewatch.start();
        latch.await();
        timewatch.end();
        System.out.println(StringUtil.format("服务端模式:{},客户端模式:{}耗时:{}", serverMode.name(), clientMode.name(), timewatch.getTotal()));
        aioServer.stop();
    }
}
