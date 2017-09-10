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
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.baseutil.collection.buffer.HeapByteBuf;
import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.client.AioClient;
import com.jfireframework.jnet.client.AioClientBuilder;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextBuilder;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.impl.MpscBufStorage;
import com.jfireframework.jnet.common.bufstorage.impl.SpscBufStorage;
import com.jfireframework.jnet.common.configuration.ChannelAttachConfiguration;
import com.jfireframework.jnet.common.configuration.MutliAttachConfiguration;
import com.jfireframework.jnet.common.configuration.MutliAttachConfiguration.MutlisAttachProcessor;
import com.jfireframework.jnet.common.configuration.SimpleConfiguration;
import com.jfireframework.jnet.common.configuration.ThreadAttchConfiguration;
import com.jfireframework.jnet.common.decodec.impl.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.streamprocessor.LengthEncodeProcessor;
import com.jfireframework.jnet.common.support.DefaultAioListener;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.AioServerBuilder;

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
    
    ChannelContextBuilder build(IoMode iomode, AioListener aioListener)
    {
        ChannelContextBuilder channelContextBuilder = null;
        switch (iomode)
        {
            case SIMPLE:
                channelContextBuilder = new ChannelContextBuilder() {
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new SimpleConfiguration(aioListener, //
                                new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = (ByteBuf<?>) data;
                                        buf.readIndex(0);
                                        return buf;
                                    }
                                }
                                }, //
                                null, //
                                10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                    }
                };
                break;
            case CHANNEL_ATTACH:
                channelContextBuilder = new ChannelContextBuilder() {
                    
                    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new ChannelAttachConfiguration(executorService, //
                                aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = (ByteBuf<?>) data;
                                        buf.readIndex(0);
                                        return buf;
                                    }
                                } }, //
                                null, //
                                10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                        
                    }
                };
                break;
            case THREAD_ATTACH:
                channelContextBuilder = new ChannelContextBuilder() {
                    ExecutorService executorService = Executors.newCachedThreadPool();
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new ThreadAttchConfiguration(executorService, //
                                aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = (ByteBuf<?>) data;
                                        buf.readIndex(0);
                                        return buf;
                                    }
                                } }, //
                                null, //
                                10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                        
                    }
                };
                break;
            case MUTLI_ATTACH:
                final MutlisAttachProcessor[] processors = new MutlisAttachProcessor[1 << 5];
                for (int i = 0; i < processors.length; i++)
                {
                    processors[i] = new MutlisAttachProcessor(aioListener);
                }
                ExecutorService executorService = Executors.newCachedThreadPool();
                for (MutlisAttachProcessor each : processors)
                {
                    executorService.submit(each);
                }
                final int mask = processors.length - 1;
                channelContextBuilder = new ChannelContextBuilder() {
                    AtomicInteger index = new AtomicInteger(0);
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        int andIncrement = index.getAndIncrement();
                        
                        Configuration configuration = new MutliAttachConfiguration(processors[andIncrement & mask], aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = (ByteBuf<?>) data;
                                        buf.readIndex(0);
                                        return buf;
                                    }
                                } }, //
                                null, //
                                10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                    }
                };
                break;
        }
        return channelContextBuilder;
    }
    
    private AioServer buildServer()
    {
        AioServerBuilder serverBuilder = new AioServerBuilder();
        serverBuilder.setBindIp("127.0.0.1");
        serverBuilder.setPort(port);
        AioListener aioListener = new DefaultAioListener();
        serverBuilder.setAioListener(aioListener);
        serverBuilder.setChannelContextBuilder(build(serverMode, aioListener));
        AioServer aioServer = serverBuilder.build();
        return aioServer;
    }
    
    private AioClientBuilder buildClient(final AtomicInteger total, final CountDownLatch latch)
    {
        AioClientBuilder clientBuilder = new AioClientBuilder();
        clientBuilder.setServerIp("127.0.0.1");
        clientBuilder.setPort(port);
        AioListener aioListener = new DefaultAioListener();
        clientBuilder.setAioListener(aioListener);
        ChannelContextBuilder channelContextBuilder = null;
        switch (clientMode)
        {
            case SIMPLE:
                channelContextBuilder = new ChannelContextBuilder() {
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new SimpleConfiguration(aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
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
                                }
                                
                                }, //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = HeapByteBuf.allocate(28);
                                        buf.addWriteIndex(4);
                                        buf.put((byte[]) data);
                                        return buf;
                                    }
                                }, new LengthEncodeProcessor(0, 4)
                                
                                }, //
                                10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                    }
                };
                break;
            case CHANNEL_ATTACH:
                channelContextBuilder = new ChannelContextBuilder() {
                    
                    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new ChannelAttachConfiguration(executorService, //
                                aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
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
                                }
                                
                                }, //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = HeapByteBuf.allocate(28);
                                        buf.addWriteIndex(4);
                                        buf.put((byte[]) data);
                                        return buf;
                                    }
                                }, new LengthEncodeProcessor(0, 4)
                                
                                }, //
                                10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                        
                    }
                };
                break;
            case THREAD_ATTACH:
                channelContextBuilder = new ChannelContextBuilder() {
                    ExecutorService executorService = Executors.newCachedThreadPool();
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        Configuration configuration = new ThreadAttchConfiguration(executorService, //
                                aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
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
                                }
                                
                                }, //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = HeapByteBuf.allocate(28);
                                        buf.addWriteIndex(4);
                                        buf.put((byte[]) data);
                                        return buf;
                                    }
                                }, new LengthEncodeProcessor(0, 4)
                                
                                }, //
                                10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                        
                    }
                };
                break;
            case MUTLI_ATTACH:
                final MutlisAttachProcessor[] processors = new MutlisAttachProcessor[1 << 5];
                for (int i = 0; i < processors.length; i++)
                {
                    processors[i] = new MutlisAttachProcessor(aioListener);
                }
                ExecutorService executorService = Executors.newCachedThreadPool();
                for (MutlisAttachProcessor each : processors)
                {
                    executorService.submit(each);
                }
                final int mask = processors.length - 1;
                channelContextBuilder = new ChannelContextBuilder() {
                    AtomicInteger index = new AtomicInteger(0);
                    
                    @Override
                    public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
                    {
                        int andIncrement = index.getAndIncrement();
                        
                        Configuration configuration = new MutliAttachConfiguration(processors[andIncrement & mask], aioListener, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
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
                                }
                                
                                }, //
                                new StreamProcessor[] { new StreamProcessor() {
                                    
                                    @Override
                                    public Object process(Object data, ChannelContext context) throws Throwable
                                    {
                                        ByteBuf<?> buf = HeapByteBuf.allocate(28);
                                        buf.addWriteIndex(4);
                                        buf.put((byte[]) data);
                                        return buf;
                                    }
                                }, new LengthEncodeProcessor(0, 4)
                                
                                }, //
                                10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
                        return configuration;
                    }
                    
                    @Override
                    public void afterContextBuild(ChannelContext serverChannelContext)
                    {
                    }
                };
                break;
            default:
                break;
        }
        clientBuilder.setChannelContextBuilder(channelContextBuilder);
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
