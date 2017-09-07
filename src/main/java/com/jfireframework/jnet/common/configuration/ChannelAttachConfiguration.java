package com.jfireframework.jnet.common.configuration;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.SpscQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;

public class ChannelAttachConfiguration extends AbstractConfiguration
{
	private ExecutorService executorService;
	
	public ChannelAttachConfiguration(ExecutorService executorService, AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
	{
		super(aioListener, frameDecodec, inProcessors, outProcessors, maxMerge, socketChannel, sendBufStorage, inCachedBuf, outCachedBuf);
		this.executorService = executorService;
		readProcessor = new ReadProcessor() {
			ChannelAttachProcessor channelAttachProcessor = new ChannelAttachProcessor();
			
			@Override
			public void process(ByteBuf<?> buf, ChannelContext channelContext) throws Throwable
			{
				ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
				channelAttachProcessor.commit(task);
			}
		};
	}
	
	class ChannelAttachProcessor implements Runnable
	{
		private static final int			IDLE			= 0;
		private static final int			WORK			= 1;
		private static final int			SPIN_THRESHOLD	= 1 << 7;
		private final Queue<ProcessorTask>	tasks			= new SpscQueue<>();
		private final CpuCachePadingInt		status			= new CpuCachePadingInt(IDLE);
		private int							spin			= 0;
		
		@Override
		public void run()
		{
			do
			{
				ProcessorTask task = tasks.poll();
				if (task == null)
				{
					spin = 0;
					for (;;)
					{
						
						if ((task = tasks.poll()) != null)
						{
							break;
						}
						else if ((spin += 1) < SPIN_THRESHOLD)
						{
							;
						}
						else
						{
							status.set(IDLE);
							if (tasks.isEmpty() == false)
							{
								tryExecute();
							}
							return;
						}
					}
				}
				try
				{
					ChannelContext channelContext = task.getChannelContext();
					Object result = ProcesserUtil.process(channelContext, inProcessors, task.getData());
					if (result instanceof ByteBuf<?>)
					{
						sendBufStorage.putBuf((ByteBuf<?>) result);
						channelContext.registerWrite();
					}
				}
				catch (Throwable e)
				{
					ChannelContext channelContext = task.getChannelContext();
					aioListener.catchException(e, channelContext);
					if (channelContext.isOpen() == false)
					{
						return;
					}
				}
			} while (true);
		}
		
		public void commit(ProcessorTask task)
		{
			tasks.offer(task);
			tryExecute();
		}
		
		private void tryExecute()
		{
			int now = status.value();
			if (now == IDLE && status.compareAndSwap(IDLE, WORK))
			{
				executorService.execute(this);
			}
		}
	}
	
}
