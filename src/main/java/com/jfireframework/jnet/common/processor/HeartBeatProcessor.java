package com.jfireframework.jnet.common.processor;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.schedule.api.Timer;
import com.jfireframework.schedule.api.Timetask;
import com.jfireframework.schedule.handler.SimpleExpireHandler;
import com.jfireframework.schedule.timer.FixedCapacityWheelTimer;
import com.jfireframework.schedule.trigger.RepeatDelayTrigger;

/**
 * 心跳检测处理器。
 * 
 * @author linbin
 *
 */
public class HeartBeatProcessor implements DataProcessor<Object>
{
	private static final Timer TIMER;
	static
	{
		TIMER = new FixedCapacityWheelTimer(1800, new SimpleExpireHandler(), Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), 1, TimeUnit.SECONDS);
	}
	private final long			heartBeatDuration;
	private volatile long		lastBeatTime;
	private static final long	OFFSET	= UNSAFE.getFieldOffset("lastBeatTime", HeartBeatProcessor.class);
	
	public HeartBeatProcessor(long heartBeatDuration, TimeUnit unit)
	{
		this.heartBeatDuration = unit.toMillis(heartBeatDuration);
	}
	
	@Override
	public void bind(final ChannelContext channelContext)
	{
		lastBeatTime = System.currentTimeMillis();
		TIMER.add(new RepeatDelayTrigger(new Timetask() {
			boolean canceled = false;
			
			@Override
			public boolean isCanceled()
			{
				return canceled;
			}
			
			@Override
			public void invoke()
			{
				long now = System.currentTimeMillis();
				if (now - lastBeatTime > heartBeatDuration)
				{
					try
					{
						channelContext.socketChannel().close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					canceled = true;
				}
			}
		}, heartBeatDuration, TimeUnit.MILLISECONDS));
	}
	
	@Override
	public boolean process(Object data, ProcessorInvoker next) throws Throwable
	{
		UNSAFE.putOrderedLong(this, OFFSET, System.currentTimeMillis());
		next.process(data);
		return true;
	}
	
}
