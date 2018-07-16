package com.jfireframework.jnet.common.processor.worker;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.util.FixArray;
import com.jfireframework.jnet.common.util.SPSCFixArray;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class FixedAttachWorker implements Runnable
{
	private static final int					IDLE			= 0;
	private static final int					WORK			= 1;
	private static final int					SPIN_THRESHOLD	= 1 << 7;
	private static final Unsafe					unsafe			= ReflectUtil.getUnsafe();
	private static final long					STATE_OFFSET	= UnsafeFieldAccess.getFieldOffset("state", FixedAttachWorker.class);
	private final ExecutorService				executorService;
	private final FixArray<FixedAttachEntity>	entities		= new SPSCFixArray<FixedAttachEntity>(512) {
																	
																	@Override
																	protected FixedAttachEntity newInstance()
																	{
																		return new FixedAttachEntity();
																	}
																};
	private int									state			= IDLE;
	
	public FixedAttachWorker(ExecutorService executorService)
	{
		this.executorService = executorService;
	}
	
	@Override
	public void run()
	{
		int spin = 0;
		do
		{
			long avail = entities.nextAvail();
			if (avail == -1)
			{
				spin = 0;
				for (;;)
				{
					
					if ((avail = entities.nextAvail()) != -1)
					{
						break;
					}
					else if ((spin += 1) < SPIN_THRESHOLD)
					{
						;
					}
					else
					{
						state = IDLE;
						if (entities.isEmpty() == false)
						{
							tryExecute();
						}
						return;
					}
				}
			}
			FixedAttachEntity slot = entities.getSlot(avail);
			ChannelContext channelContext = slot.channelContext;
			ProcessorInvoker invoker = slot.invoker;
			Object data = slot.data;
			try
			{
				entities.comsumeAvail(avail);
				invoker.process(data);
			}
			catch (Throwable e)
			{
				try
				{
					channelContext.socketChannel().close();
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
		long offerIndexAvail = entities.waitUntilOfferIndexAvail();
		FixedAttachEntity slot = entities.getSlot(offerIndexAvail);
		slot.channelContext = channelContext;
		slot.invoker = invoker;
		slot.data = data;
		entities.commit(offerIndexAvail);
		tryExecute();
	}
	
	private void tryExecute()
	{
		int now = state;
		if (now == IDLE && unsafe.compareAndSwapLong(this, STATE_OFFSET, IDLE, WORK))
		{
			executorService.execute(this);
		}
	}
	
	class FixedAttachEntity
	{
		ChannelContext		channelContext;
		ProcessorInvoker	invoker;
		Object				data;
	}
}
