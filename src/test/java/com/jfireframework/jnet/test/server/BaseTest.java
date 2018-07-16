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
import com.jfireframework.jnet.client.AioClient;
import com.jfireframework.jnet.client.AioClientBuilder;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoderByHeap;
import com.jfireframework.jnet.common.internal.DefaultAioListener;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.processor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.processor.CommonPoolProcessor;
import com.jfireframework.jnet.common.processor.MutliAttachIoProcessor;
import com.jfireframework.jnet.common.processor.ThreadAttachIoProcessor;
import com.jfireframework.jnet.common.processor.worker.FixedAttachWorker;
import com.jfireframework.jnet.common.util.Allocator;
import com.jfireframework.jnet.common.util.ReadProcessorAdapter;
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
	private int								clientThreadNum	= 10;
	private int								sendCount		= 10000;
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
		        { IoMode.SIMPLE, IoMode.COMMON_POOL }, //
		        { IoMode.THREAD_ATTACH, IoMode.SIMPLE }, //
		        { IoMode.THREAD_ATTACH, IoMode.THREAD_ATTACH }, //
		        { IoMode.THREAD_ATTACH, IoMode.CHANNEL_ATTACH }, //
		        { IoMode.THREAD_ATTACH, IoMode.MUTLI_ATTACH }, //
		        { IoMode.THREAD_ATTACH, IoMode.COMMON_POOL }, //
		        { IoMode.CHANNEL_ATTACH, IoMode.SIMPLE }, //
		        { IoMode.CHANNEL_ATTACH, IoMode.THREAD_ATTACH }, //
		        { IoMode.CHANNEL_ATTACH, IoMode.CHANNEL_ATTACH }, //
		        { IoMode.CHANNEL_ATTACH, IoMode.MUTLI_ATTACH }, //
		        { IoMode.CHANNEL_ATTACH, IoMode.COMMON_POOL }, //
		        { IoMode.MUTLI_ATTACH, IoMode.SIMPLE }, //
		        { IoMode.MUTLI_ATTACH, IoMode.THREAD_ATTACH }, //
		        { IoMode.MUTLI_ATTACH, IoMode.CHANNEL_ATTACH }, //
		        { IoMode.MUTLI_ATTACH, IoMode.MUTLI_ATTACH }, //
		        { IoMode.MUTLI_ATTACH, IoMode.COMMON_POOL }, //
		        { IoMode.COMMON_POOL, IoMode.SIMPLE }, //
		        { IoMode.COMMON_POOL, IoMode.THREAD_ATTACH }, //
		        { IoMode.COMMON_POOL, IoMode.CHANNEL_ATTACH }, //
		        { IoMode.COMMON_POOL, IoMode.MUTLI_ATTACH }, //
		        { IoMode.COMMON_POOL, IoMode.COMMON_POOL }, //
		
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
	
	ChannelConnectListener build(IoMode iomode, AioListener aioListener)
	{
		ChannelConnectListener channelContextBuilder = null;
		final DataProcessor<PooledIoBuffer> businessProcessor = new ReadProcessorAdapter<PooledIoBuffer>() {
			final String traceId = TRACEID.newTraceId();
			
			@Override
			public void process(PooledIoBuffer buf, ProcessorChain chain, ChannelContext channelContext)
			{
				logger.debug("traceId:{} 服务端收到消息,{}", traceId, buf);
				buf.setReadPosi(0);
				channelContext.write(buf);
			}
		};
		switch (iomode)
		{
			case SIMPLE:
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        businessProcessor);
					}
					
				};
				break;
			case CHANNEL_ATTACH:
				serverExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new ChannelAttachProcessor(serverExecutor), //
						        businessProcessor);
					}
					
				};
				break;
			case THREAD_ATTACH:
				serverExecutor = Executors.newCachedThreadPool();
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new ThreadAttachIoProcessor(serverExecutor), //
						        businessProcessor);
					}
					
				};
				break;
			case MUTLI_ATTACH:
				final FixedAttachWorker[] processors = new FixedAttachWorker[1 << 5];
				for (int i = 0; i < processors.length; i++)
				{
					processors[i] = new FixedAttachWorker();
				}
				serverExecutor = Executors.newCachedThreadPool();
				for (FixedAttachWorker each : processors)
				{
					serverExecutor.submit(each);
				}
				final int mask = processors.length - 1;
				channelContextBuilder = new ChannelConnectListener() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new MutliAttachIoProcessor(processors[andIncrement & mask]), //
						        businessProcessor);
					}
					
				};
				break;
			case COMMON_POOL:
				serverExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 500), //
						        new CommonPoolProcessor(serverExecutor), //
						        businessProcessor);
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
		serverBuilder.setChannelConnectListener(build(serverIoMode, aioListener));
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
		ChannelConnectListener channelContextBuilder = null;
		final DataProcessor<PooledIoBuffer> businessProcessor = new ReadProcessorAdapter<PooledIoBuffer>() {
			final String traceId = TRACEID.newTraceId();
			
			@Override
			public void process(PooledIoBuffer buf, ProcessorChain chain, ChannelContext channelContext)
			{
				try
				{
					logger.debug("traceId:{} 单位端收到消息:{}", traceId, buf);
					Integer value = buf.getInt();
					sendContent[value] += 1;
					buf.release();
					latch.countDown();
				}
				catch (Exception e)
				{
					logger.error("traceId:{} 单位端收到消息:{}", traceId, buf, e);
					
				}
			}
		};
		switch (clientIoMode)
		{
			case SIMPLE:
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						ChannelContext channelContext = new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), businessProcessor //
						);
						return channelContext;
					}
					
				};
				break;
			case CHANNEL_ATTACH:
				clientExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						ChannelContext channelContext = new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new ChannelAttachProcessor(clientExecutor), //
						        businessProcessor);
						return channelContext;
					}
					
				};
				break;
			case THREAD_ATTACH:
				clientExecutor = Executors.newCachedThreadPool();
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new ThreadAttachIoProcessor(clientExecutor), //
						        businessProcessor
						//
						);
					}
					
				};
				break;
			case MUTLI_ATTACH:
				final FixedAttachWorker[] processors = new FixedAttachWorker[1 << 5];
				for (int i = 0; i < processors.length; i++)
				{
					processors[i] = new FixedAttachWorker();
				}
				clientExecutor = Executors.newCachedThreadPool();
				for (FixedAttachWorker each : processors)
				{
					clientExecutor.submit(each);
				}
				final int mask = processors.length - 1;
				channelContextBuilder = new ChannelConnectListener() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						return new DefaultChannelContext(socketChannel, mask, aioListener, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new MutliAttachIoProcessor(processors[andIncrement & mask]), //
						        businessProcessor);
					}
					
				};
				break;
			case COMMON_POOL:
				clientExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoderByHeap(0, 4, 4, 500), //
						        new CommonPoolProcessor(clientExecutor), //
						        businessProcessor
						//
						);
					}
				};
				break;
			default:
				break;
		}
		clientBuilder.setChannelConnectListener(channelContextBuilder);
		AioClient client = clientBuilder.build();
		client.connect();
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
								PooledIoBuffer buf = Allocator.allocate(100);
								// 报文长度是8
								buf.putInt(8);
								buf.putInt(value);
								client.write(buf);
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
