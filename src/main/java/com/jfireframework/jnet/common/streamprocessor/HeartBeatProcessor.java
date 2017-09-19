package com.jfireframework.jnet.common.streamprocessor;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.schedule.api.Timer;
import com.jfireframework.schedule.api.Timetask;
import com.jfireframework.schedule.handler.SimpleExpireHandler;
import com.jfireframework.schedule.timer.FixedCapacityWheelTimer;
import com.jfireframework.schedule.trigger.RepeatDelayTrigger;

/**
 * 心跳检测处理器。
 * @author linbin
 *
 */
public class HeartBeatProcessor implements StreamProcessor
{
	private static final Timer TIMER;
	static
	{
		TIMER = new FixedCapacityWheelTimer(1800, new SimpleExpireHandler(), Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), 1, TimeUnit.SECONDS);
	}
	private final int		heartBeatDuration;
	private volatile long	lastBeatTime;
	
	public HeartBeatProcessor(int heartBeatDuration)
	{
		this.heartBeatDuration = heartBeatDuration;
	}
	
	@Override
	public Object process(Object data, ChannelContext context) throws Throwable
	{
		lastBeatTime = System.currentTimeMillis();
		return data;
	}
	
	@Override
	public void initialize(final ChannelContext channelContext)
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
					channelContext.close();
					canceled = true;
				}
			}
		}, heartBeatDuration, TimeUnit.SECONDS));
	}
	
}
