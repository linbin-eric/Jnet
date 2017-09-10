package com.jfireframework.jnet.test.server;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.baseutil.TRACEID;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.baseutil.collection.buffer.HeapByteBuf;
import com.jfireframework.jnet.client.AioClient;
import com.jfireframework.jnet.client.AioClientBuilder;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextBuilder;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.configuration.ConfigurationTemplate;
import com.jfireframework.jnet.common.configuration.MutliAttachConfiguration.MutlisAttachProcessor;
import com.jfireframework.jnet.common.decodec.impl.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.decodec.impl.TotalLengthFieldBasedFrameDecoderByHeap;
import com.jfireframework.jnet.common.streamprocessor.LengthEncodeProcessor;
import com.jfireframework.jnet.common.support.DefaultAioListener;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.AioServerBuilder;

/**
 * 基本测试。用于验证代码的正确性
 * 
 * @author linbin
 *
 */
@RunWith(Parameterized.class)
public class BaseTest
{
	private int								port			= 8546;
	private int								clientThreadNum	= 30;
	private int								sendCount		= 1000000;
	private CountDownLatch					latch			= new CountDownLatch(sendCount);
	private ConcurrentLinkedQueue<Integer>	waitForSend		= new ConcurrentLinkedQueue<>();
	private int[]							sendContent;
	private ExecutorService					serverExecutor;
	private ExecutorService					clientExecutor;
	private IoMode							clientIoMode;
	private IoMode							serverIoMode;
	private static final Logger				logger			= LoggerFactory.getLogger(BaseTest.class);
	
	public BaseTest(IoMode serverIoMode, IoMode clientIoMode)
	{
		this.serverIoMode = serverIoMode;
		this.clientIoMode = clientIoMode;
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
		sendContent = new int[sendCount];
		for (int i = 0; i < sendCount; i++)
		{
			waitForSend.offer(Integer.valueOf(i));
		}
		logger.info("开始测试，服务端模式:{},客户端模式:{}", serverIoMode, clientIoMode);
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
						final String traceId = TRACEID.newTraceId();
						Configuration configuration = ConfigurationTemplate.simple(new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        logger.debug("traceId:{} 服务端收到消息", traceId);
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        buf.readIndex(0);
								        return buf;
							        }
						        }
						        }, //
						        null, socketChannel);
						return configuration;
					}
					
					@Override
					public void afterContextBuild(ChannelContext serverChannelContext)
					{
					}
				};
				break;
			case CHANNEL_ATTACH:
				serverExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
				channelContextBuilder = new ChannelContextBuilder() {
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						Configuration configuration = ConfigurationTemplate.channelAttch(serverExecutor, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        buf.readIndex(0);
								        return buf;
							        }
						        } }, //
						        null, socketChannel);
						return configuration;
					}
					
					@Override
					public void afterContextBuild(ChannelContext serverChannelContext)
					{
						
					}
				};
				break;
			case THREAD_ATTACH:
				serverExecutor = Executors.newCachedThreadPool();
				channelContextBuilder = new ChannelContextBuilder() {
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						Configuration configuration = ConfigurationTemplate.threadAttch(serverExecutor, new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        buf.readIndex(0);
								        return buf;
							        }
						        } }, //
						        null, socketChannel);
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
				serverExecutor = Executors.newCachedThreadPool();
				for (MutlisAttachProcessor each : processors)
				{
					serverExecutor.submit(each);
				}
				final int mask = processors.length - 1;
				channelContextBuilder = new ChannelContextBuilder() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						
						Configuration configuration =
						        
						        ConfigurationTemplate.mutliAttch(processors[andIncrement & mask], new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						                new StreamProcessor[] { new StreamProcessor() {
							                
							                @Override
							                public Object process(Object data, ChannelContext context) throws Throwable
							                {
								                ByteBuf<?> buf = (ByteBuf<?>) data;
								                buf.readIndex(0);
								                return buf;
							                }
						                } }, //
						                null, socketChannel);
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
		AioListener aioListener = new DefaultAioListener();
		AioServerBuilder serverBuilder = new AioServerBuilder();
		serverBuilder.setAioListener(aioListener);
		serverBuilder.setBindIp("127.0.0.1");
		serverBuilder.setPort(port);
		serverBuilder.setChannelContextBuilder(build(serverIoMode, aioListener));
		AioServer aioServer = serverBuilder.build();
		return aioServer;
	}
	
	private AioClient buildClient()
	{
		
		AioListener aioListener = new DefaultAioListener();
		AioClientBuilder clientBuilder = new AioClientBuilder();
		clientBuilder.setServerIp("127.0.0.1");
		clientBuilder.setPort(port);
		clientBuilder.setAioListener(aioListener);
		ChannelContextBuilder channelContextBuilder = null;
		switch (clientIoMode)
		{
			case SIMPLE:
				channelContextBuilder = new ChannelContextBuilder() {
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						Configuration configuration =
						        
						        ConfigurationTemplate.simple(new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						                new StreamProcessor[] { new StreamProcessor() {
							                
							                final String traceId = TRACEID.newTraceId();
							                
							                @Override
							                public Object process(Object data, ChannelContext context) throws Throwable
							                {
								                ByteBuf<?> buf = (ByteBuf<?>) data;
								                Integer value = buf.readInt();
								                sendContent[value] += 1;
								                logger.debug("traceId:{} 单位端收到消息:{}", traceId, value);
								                buf.release();
								                latch.countDown();
								                return null;
							                }
						                }
										
										}, //
						                new StreamProcessor[] { new StreamProcessor() {
							                final String traceId = TRACEID.newTraceId();
							                
							                @Override
							                public Object process(Object data, ChannelContext context) throws Throwable
							                {
								                Integer str = (Integer) data;
								                logger.debug("traceId:{} 单位端发出消息:{}", traceId, str);
								                ByteBuf<?> buf = HeapByteBuf.allocate(100);
								                buf.addWriteIndex(4);
								                buf.writeInt(str);
								                return buf;
							                }
						                }, new LengthEncodeProcessor(0, 4)
										
										}, socketChannel);
						return configuration;
					}
					
					@Override
					public void afterContextBuild(ChannelContext serverChannelContext)
					{
					}
				};
				break;
			case CHANNEL_ATTACH:
				clientExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
				channelContextBuilder = new ChannelContextBuilder() {
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						Configuration configuration = ConfigurationTemplate.channelAttch(clientExecutor, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        Integer value = buf.readInt();
								        sendContent[value] += 1;
								        buf.release();
								        latch.countDown();
								        return null;
							        }
						        }
								
								}, //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        Integer str = (Integer) data;
								        ByteBuf<?> buf = HeapByteBuf.allocate(100);
								        buf.addWriteIndex(4);
								        buf.writeInt(str);
								        return buf;
							        }
						        }, new LengthEncodeProcessor(0, 4)
								
								}, //
						        socketChannel);
						return configuration;
					}
					
					@Override
					public void afterContextBuild(ChannelContext serverChannelContext)
					{
						
					}
				};
				break;
			case THREAD_ATTACH:
				clientExecutor = Executors.newCachedThreadPool();
				channelContextBuilder = new ChannelContextBuilder() {
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						Configuration configuration = ConfigurationTemplate.threadAttch(clientExecutor, new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        Integer value = buf.readInt();
								        sendContent[value] += 1;
								        buf.release();
								        latch.countDown();
								        return null;
							        }
						        }
								
								}, //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        Integer str = (Integer) data;
								        ByteBuf<?> buf = HeapByteBuf.allocate(100);
								        buf.addWriteIndex(4);
								        buf.writeInt(str);
								        return buf;
							        }
						        }, new LengthEncodeProcessor(0, 4)
								
								}, socketChannel);
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
				clientExecutor = Executors.newCachedThreadPool();
				for (MutlisAttachProcessor each : processors)
				{
					clientExecutor.submit(each);
				}
				final int mask = processors.length - 1;
				channelContextBuilder = new ChannelContextBuilder() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						
						Configuration configuration = ConfigurationTemplate.mutliAttch(processors[andIncrement & mask], new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        ByteBuf<?> buf = (ByteBuf<?>) data;
								        Integer value = buf.readInt();
								        sendContent[value] += 1;
								        buf.release();
								        latch.countDown();
								        return null;
							        }
						        }
								
								}, //
						        new StreamProcessor[] { new StreamProcessor() {
							        
							        @Override
							        public Object process(Object data, ChannelContext context) throws Throwable
							        {
								        Integer str = (Integer) data;
								        ByteBuf<?> buf = DirectByteBuf.allocate(100);
								        buf.addWriteIndex(4);
								        buf.writeInt(str);
								        return buf;
							        }
						        }, new LengthEncodeProcessor(0, 4)
								
								}, socketChannel);
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
		AioClient client = clientBuilder.build();
		return client;
	}
	
	@Test
	public void test() throws Throwable
	{
		
		AioServer aioServer = buildServer();
		aioServer.start();
		ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
			int i = 0;
			
			@Override
			public Thread newThread(Runnable r)
			{
				return new Thread(r, "测试线程" + (i++));
			}
		});
		for (int i = 0; i < clientThreadNum; i++)
		{
			executorService.execute(new Runnable() {
				
				@Override
				public void run()
				{
					final AioClient client = buildClient();
					client.connect();
					do
					{
						Integer value = waitForSend.poll();
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
			Assert.assertEquals(1, sendContent[i]);
		}
		aioServer.stop();
		if (serverExecutor != null)
		{
			serverExecutor.shutdownNow();
		}
		if (clientExecutor != null)
		{
			clientExecutor.shutdownNow();
		}
		logger.info("测试成功，服务端模式:{},客户端模式:{}", serverIoMode, clientIoMode);
	}
}
