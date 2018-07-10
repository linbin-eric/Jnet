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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.baseutil.StringUtil;
import com.jfireframework.baseutil.time.Timewatch;
import com.jfireframework.jnet.client.AioClient;
import com.jfireframework.jnet.client.AioClientBuilder;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.processor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.processor.CommonPoolProcessor;
import com.jfireframework.jnet.common.processor.MutliAttachIoProcessor;
import com.jfireframework.jnet.common.processor.ThreadAttachIoProcessor;
import com.jfireframework.jnet.common.processor.worker.MutlisAttachWorker;
import com.jfireframework.jnet.common.support.DefaultAioListener;
import com.jfireframework.jnet.common.support.DefaultChannelContext;
import com.jfireframework.jnet.common.util.Allocator;
import com.jfireframework.jnet.common.util.ReadProcessorAdapter;
import com.jfireframework.jnet.server.AioServer;
import com.jfireframework.jnet.server.AioServerBuilder;

@RunWith(Parameterized.class)
public class SpeedTest
{
	private int					port			= 8546;
	private int					clientThreadNum	= 30;
	private int					sendCount		= 50000;
	private int					sum				= clientThreadNum * sendCount;
	private IoMode				serverMode;
	private IoMode				clientMode;
	private static final Logger	logger			= LoggerFactory.getLogger(SpeedTest.class);
	
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
	
	ChannelConnectListener build(IoMode iomode, AioListener aioListener)
	{
		ChannelConnectListener channelContextBuilder = null;
		final DataProcessor<PooledIoBuffer> processor = new ReadProcessorAdapter<PooledIoBuffer>() {
			
			@Override
			public void process(PooledIoBuffer buf, ProcessorChain chain, ChannelContext channelContext)
			{
				logger.debug("收到客户端消息,{}", buf.toString());
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
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        processor);
					}
					
				};
				break;
			case CHANNEL_ATTACH:
				channelContextBuilder = new ChannelConnectListener() {
					
					ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new ChannelAttachProcessor(executorService), //
						        processor);
					}
					
				};
				break;
			case THREAD_ATTACH:
				channelContextBuilder = new ChannelConnectListener() {
					ExecutorService executorService = Executors.newCachedThreadPool();
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new ThreadAttachIoProcessor(executorService), //
						        processor);
					}
					
				};
				break;
			case MUTLI_ATTACH:
				final MutlisAttachWorker[] workers = new MutlisAttachWorker[1 << 5];
				for (int i = 0; i < workers.length; i++)
				{
					workers[i] = new MutlisAttachWorker();
				}
				ExecutorService executorService = Executors.newCachedThreadPool();
				for (MutlisAttachWorker each : workers)
				{
					executorService.submit(each);
				}
				final int mask = workers.length - 1;
				channelContextBuilder = new ChannelConnectListener() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new MutliAttachIoProcessor(workers[andIncrement & mask]), //
						        processor);
					}
					
				};
				break;
			case COMMON_POOL:
				channelContextBuilder = new ChannelConnectListener() {
					
					ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new CommonPoolProcessor(executorService), //
						        processor);
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
		serverBuilder.setChannelConnectListener(build(serverMode, aioListener));
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
		ChannelConnectListener channelContextBuilder = null;
		final DataProcessor<PooledIoBuffer> processor = new ReadProcessorAdapter<PooledIoBuffer>() {
			
			@Override
			public void process(PooledIoBuffer buf, ProcessorChain chain, ChannelContext channelContext)
			{
				buf.release();
				int now = total.incrementAndGet();
				if (now == sum)
				{
					latch.countDown();
				}
			}
		};
		switch (clientMode)
		{
			case SIMPLE:
				channelContextBuilder = new ChannelConnectListener() {
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        processor);
					}
					
				};
				break;
			case CHANNEL_ATTACH:
				channelContextBuilder = new ChannelConnectListener() {
					
					ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new ChannelAttachProcessor(executorService), //
						        processor);
					}
					
				};
				break;
			case THREAD_ATTACH:
				channelContextBuilder = new ChannelConnectListener() {
					ExecutorService executorService = Executors.newCachedThreadPool();
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new ThreadAttachIoProcessor(executorService), //
						        processor);
					}
					
				};
				break;
			case MUTLI_ATTACH:
				final MutlisAttachWorker[] processors = new MutlisAttachWorker[1 << 5];
				for (int i = 0; i < processors.length; i++)
				{
					processors[i] = new MutlisAttachWorker();
				}
				ExecutorService executorService = Executors.newCachedThreadPool();
				for (MutlisAttachWorker each : processors)
				{
					executorService.submit(each);
				}
				final int mask = processors.length - 1;
				channelContextBuilder = new ChannelConnectListener() {
					AtomicInteger index = new AtomicInteger(0);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						int andIncrement = index.getAndIncrement();
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new MutliAttachIoProcessor(processors[andIncrement & mask]), //
						        processor);
					}
					
				};
				break;
			case COMMON_POOL:
				channelContextBuilder = new ChannelConnectListener() {
					
					ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);
					
					@Override
					public ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener)
					{
						return new DefaultChannelContext(socketChannel, 10, aioListener, //
						        new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 5000000), //
						        new CommonPoolProcessor(executorService), //
						        processor);
					}
					
				};
				break;
			default:
				break;
		}
		clientBuilder.setChannelConnectListener(channelContextBuilder);
		return clientBuilder;
	}
	
	@Test
	public void test() throws Throwable
	{
		for (int i = 0; i < 2; i++)
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
							PooledIoBuffer buf = Allocator.allocate(128);
							buf.putInt(content.length + 4);
							buf.put(content);
							client.write(buf);
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
