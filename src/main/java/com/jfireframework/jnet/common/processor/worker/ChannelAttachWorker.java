package com.jfireframework.jnet.common.processor.worker;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.SpscQueue;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorInvoker;

public class ChannelAttachWorker implements Runnable
{
	
	private static final int					IDLE			= 0;
	private static final int					WORK			= 1;
	private static final int					SPIN_THRESHOLD	= 1 << 7;
	private final ExecutorService				executorService;
	private final Queue<ChannelAttachEntity>	entities		= new SpscQueue<>();
	private final CpuCachePadingInt				status			= new CpuCachePadingInt(IDLE);
	
	public ChannelAttachWorker(ExecutorService executorService)
	{
		this.executorService = executorService;
	}
	
	@Override
	public void run()
	{
		int spin = 0;
		do
		{
			ChannelAttachEntity entity = entities.poll();
			if (entity == null)
			{
				spin = 0;
				for (;;)
				{
					
					if ((entity = entities.poll()) != null)
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
						if (entities.isEmpty() == false)
						{
							tryExecute();
						}
						return;
					}
				}
			}
			try
			{
				entity.invoker.process(entity.data);
			}
			catch (Throwable e)
			{
				try
				{
					entity.channelContext.socketChannel().close();
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
			}
		} while (true);
	}
	
	public void commit(ChannelContext channelContext, ProcessorInvoker invoker, Object data)
	{
		entities.offer(new ChannelAttachEntity(channelContext, invoker, data));
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
	
	class ChannelAttachEntity
	{
		ChannelContext		channelContext;
		ProcessorInvoker	invoker;
		Object				data;
		
		public ChannelAttachEntity(ChannelContext channelContext, ProcessorInvoker invoker, Object data)
		{
			this.channelContext = channelContext;
			this.invoker = invoker;
			this.data = data;
		}
		
	}
}
