package com.jfireframework.jnet.test.server;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfireframework.jnet.client.AioClient;
import com.jfireframework.jnet.client.DefaultClient;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledBufferAllocator;
import com.jfireframework.jnet.common.buffer.UnPooledRecycledBufferAllocator;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.internal.DefaultAcceptHandler;
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
	private AioServer			aioServer;
	private String				ip				= "127.0.0.1";
	private int					port			= 7598;
	private int					numPerThread	= 10;
	private int					numClients		= 1;
	private AioClient[]			clients;
	private CountDownLatch[]	latchs;
	private int[]				sendContent;
	private int[][]				results;
	private BufferAllocator		allocator		= new UnPooledRecycledBufferAllocator();
	private static final Logger	logger			= LoggerFactory.getLogger(BaseTest.class);
	
	@Parameters
	public static Collection<Object[]> params()
	{
		return Arrays.asList(new Object[][] { //
		        { new PooledBufferAllocator(), 0 }, //
				// { new PooledBufferAllocator(), 1 }
				
		});
	}
	
	public BaseTest(final BufferAllocator bufferAllocator, int batchWriteNum)
	{
		clients = new AioClient[numClients];
		results = new int[numClients][numPerThread];
		latchs = new CountDownLatch[numClients];
		for (int i = 0; i < numClients; i++)
		{
			results[i] = new int[numPerThread];
			Arrays.fill(results[i], -1);
			latchs[i] = new CountDownLatch(numPerThread);
		}
		sendContent = new int[numPerThread];
		for (int i = 0; i < numPerThread; i++)
		{
			sendContent[i] = i;
		}
		for (int i = 0; i < numClients; i++)
		{
			final int index = i;
			clients[i] = new DefaultClient(new ChannelContextInitializer() {
				
				@Override
				public void onChannelContextInit(ChannelContext channelContext)
				{
					channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1000, bufferAllocator), //
					        new DataProcessor<IoBuffer>() {
						        CountDownLatch latch = latchs[index];
						        int[]	result	= results[index];
						        
						        @Override
						        public void bind(ChannelContext channelContext)
						        {
							        ;
						        }
						        
						        @Override
						        public void process(IoBuffer buffer, ProcessorInvoker next) throws Throwable
						        {
							        int j = buffer.getInt();
							        result[j] = j;
							        System.out.println("读取到" + j);
							        latch.countDown();
						        }
					        });
				}
			}, ip, port, null, bufferAllocator);
		}
		AioServerBuilder builder = new AioServerBuilder();
		builder.setAcceptHandler(new DefaultAcceptHandler(null, bufferAllocator, batchWriteNum, 512, new ChannelContextInitializer() {
			
			@Override
			public void onChannelContextInit(ChannelContext channelContext)
			{
				channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 100, bufferAllocator), new DataProcessor<IoBuffer>() {
					private ChannelContext channelContext;
					
					@Override
					public void bind(ChannelContext channelContext)
					{
						this.channelContext = channelContext;
					}
					
					@Override
					public void process(IoBuffer buffer, ProcessorInvoker next) throws Throwable
					{
						buffer.addReadPosi(-4);
						channelContext.write(buffer);
					}
				});
			}
		}));
		builder.setBindIp(ip);
		builder.setPort(port);
		aioServer = builder.build();
		aioServer.start();
	}
	
	@Test
	public void test()
	{
		final CyclicBarrier barrier = new CyclicBarrier(numClients);
		for (int i = 0; i < numClients; i++)
		{
			final int index = i;
			new Thread(new Runnable() {
				
				@Override
				public void run()
				{
					AioClient client = clients[index];
					try
					{
						barrier.await();
					}
					catch (InterruptedException | BrokenBarrierException e1)
					{
						e1.printStackTrace();
					}
					for (int j : sendContent)
					{
						IoBuffer buffer = allocator.ioBuffer(8);
						buffer.putInt(8);
						buffer.putInt(j);
						try
						{
							client.write(buffer);
						}
						catch (Exception e)
						{
							;
						}
					}
				}
			}).start();
		}
		for (CountDownLatch each : latchs)
		{
			try
			{
				each.await(10000, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (int[] each : results)
		{
			for (int i = 0; i < numPerThread; i++)
			{
				assertEquals(i, each[i]);
			}
		}
		for (AioClient each : clients)
		{
			each.close();
		}
		aioServer.termination();
		logger.info("测试完毕");
	}
	
}
