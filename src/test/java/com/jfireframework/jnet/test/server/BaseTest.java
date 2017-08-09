package com.jfireframework.jnet.test.server;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.client.build.AioClientBuilder;
import com.jfireframework.jnet.client.client.AioClient;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.bufstorage.impl.MpscBufStorage;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.common.decodec.impl.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.streamprocessor.LengthEncodeProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorIndexFlag;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.build.AioServerBuilder;

/**
 * 基本测试。用于验证代码的正确性
 * 
 * @author linbin
 *
 */
@RunWith(Parameterized.class)
public class BaseTest
{
    private int                               port            = 8546;
    private int                               clientThreadNum = 10;
    private int                               sendCount       = 50000;
    private CountDownLatch                    latch           = new CountDownLatch(sendCount);
    private ConcurrentHashMap<String, String> res             = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<String>     waitForSend     = new ConcurrentLinkedQueue<>();
    private String[]                          sendContent;
    private volatile boolean                  error           = false;
    private IoMode                            clientIoMode;
    private IoMode                            serverIoMode;
    private static final Logger               logger          = LoggerFactory.getLogger(BaseTest.class);
    
    public BaseTest(IoMode serverIoMode, IoMode clientIoMode)
    {
        this.serverIoMode = serverIoMode;
        this.clientIoMode = clientIoMode;
        logger.debug("开始测试，服务端模式:{},客户端模式:{}", serverIoMode, clientIoMode);
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
    
    @Before
    public void before()
    {
        res.clear();
        sendContent = new String[sendCount];
        for (int i = 0; i < sendCount; i++)
        {
            String value = "nihao" + i;
            sendContent[i] = value;
            waitForSend.offer(value);
        }
    }
    
    private AioServer buildServer()
    {
        AioServerBuilder serverBuilder = new AioServerBuilder();
        serverBuilder.setBindIp("127.0.0.1");
        serverBuilder.setPort(port);
        serverBuilder.setIoMode(serverIoMode);
        serverBuilder.setChannelContextBuilder(new ChannelContextBuilder() {
            
            @Override
            public ChannelContextConfig onConnect(AsynchronousSocketChannel socketChannel)
            {
                ChannelContextConfig config = new ChannelContextConfig();
                config.setBufStorage(new MpscBufStorage());
                config.setFrameDecodec(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500));
                config.setMaxMerge(10);
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
    
    private AioClient buildClient()
    {
        AioClientBuilder clientBuilder = new AioClientBuilder();
        clientBuilder.setServerIp("127.0.0.1");
        clientBuilder.setPort(port);
        clientBuilder.setIoMode(clientIoMode);
        clientBuilder.setChannelContextBuilder(new ChannelContextBuilder() {
            
            @Override
            public ChannelContextConfig onConnect(AsynchronousSocketChannel socketChannel)
            {
                ChannelContextConfig config = new ChannelContextConfig();
                config.setFrameDecodec(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500));
                config.setMaxMerge(10);
                config.setBufStorage(new MpscBufStorage());
                config.setInProcessors(new StreamProcessor() {
                    
                    @Override
                    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
                    {
                        ByteBuf<?> buf = (ByteBuf<?>) data;
                        String value = buf.readString();
                        if (res.putIfAbsent(value, "") != null)
                        {
                            error = true;
                        }
                        buf.release();
                        latch.countDown();
                        return null;
                    }
                });
                config.setOutProcessors(new StreamProcessor() {
                    
                    @Override
                    public Object process(Object data, ProcessorIndexFlag result, ChannelContext context) throws Throwable
                    {
                        String str = (String) data;
                        ByteBuf<?> buf = DirectByteBuf.allocate(100);
                        buf.addWriteIndex(4);
                        buf.writeString(str);
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
        AioClient client = clientBuilder.build();
        return client;
    }
    
    @Test
    public void test() throws Throwable
    {
        AioServer aioServer = buildServer();
        aioServer.start();
        final AioClient client = buildClient();
        client.connect();
        ExecutorService executorService = Executors.newFixedThreadPool(clientThreadNum);
        for (int i = 0; i < clientThreadNum; i++)
        {
            executorService.execute(new Runnable() {
                
                @Override
                public void run()
                {
                    do
                    {
                        String value = waitForSend.poll();
                        if (value != null)
                        {
                            try
                            {
                                client.write(value);
                            }
                            catch (Throwable e)
                            {
                                e.printStackTrace();
                            }
                        }
                    } while (waitForSend.isEmpty() == false);
                }
            });
        }
        latch.await();
        for (int i = 0; i < sendCount; i++)
        {
            Assert.assertTrue(res.containsKey(sendContent[i]));
        }
        Assert.assertFalse(error);
        aioServer.stop();
        logger.error("测试成功，服务端模式:{},客户端模式:{}", serverIoMode, clientIoMode);
    }
}
