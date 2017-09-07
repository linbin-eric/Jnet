package com.jfireframework.jnet.common.configuration;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;

public class MutliAttachConfiguration extends AbstractConfiguration
{
	
	public MutliAttachConfiguration(final MutlisAttachProcessor mutlisAttachProcessor, AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
	{
		super(aioListener, frameDecodec, inProcessors, outProcessors, maxMerge, socketChannel, sendBufStorage, inCachedBuf, outCachedBuf);
		readProcessor = new ReadProcessor() {
			
			@Override
			public void process(ByteBuf<?> buf, ChannelContext channelContext) throws Throwable
			{
				ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
				mutlisAttachProcessor.commit(task);
			}
		};
	}
	
	public static class MutlisAttachProcessor implements Runnable
	{
		private final Queue<ProcessorTask>	tasks			= new MPSCQueue<>();
		private static final int			IDLE			= 0;
		private static final int			WORK			= 1;
		private final CpuCachePadingInt		status			= new CpuCachePadingInt(WORK);
		private final AioListener			serverListener;
		private static final int			SPIN_THRESHOLD	= 1 << 7;
		private int							spin			= 0;
		private volatile Thread				owner;
		
		public MutlisAttachProcessor(AioListener serverListener)
		{
			this.serverListener = serverListener;
		}
		
		@Override
		public void run()
		{
			status.set(WORK);
			owner = Thread.currentThread();
			termination: do
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
							spin = 0;
							status.set(IDLE);
							if ((task = tasks.poll()) != null)
							{
								status.set(WORK);
								break;
							}
							else
							{
								LockSupport.park();
								status.set(WORK);
								if (Thread.currentThread().isInterrupted())
								{
									break termination;
								}
							}
						}
					}
				}
				try
				{
					ChannelContext serverChannelContext = task.getChannelContext();
					if (serverChannelContext.isOpen())
					{
						Object result = ProcesserUtil.process(serverChannelContext, serverChannelContext.inProcessors(), task.getData());
						if (result instanceof ByteBuf<?>)
						{
							serverChannelContext.sendBufStorage().putBuf((ByteBuf<?>) result);
							serverChannelContext.registerWrite();
						}
					}
				}
				catch (Throwable e)
				{
					serverListener.catchException(e, task.getChannelContext());
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
			if (now == IDLE)
			{
				LockSupport.unpark(owner);
			}
		}
	}
	
}
