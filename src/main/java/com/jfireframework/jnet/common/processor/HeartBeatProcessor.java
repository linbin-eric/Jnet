package com.jfireframework.jnet.common.processor;

import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.schedule.api.Timer;
import com.jfireframework.schedule.api.Timetask;
import com.jfireframework.schedule.handler.SimpleExpireHandler;
import com.jfireframework.schedule.timer.FixedCapacityWheelTimer;
import com.jfireframework.schedule.trigger.RepeatDelayTrigger;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 心跳检测处理器。
 *
 * @author linbin
 */
public class HeartBeatProcessor extends BindDownAndUpStreamDataProcessor<Object>
{
    private static final Timer TIMER;
    private static final long OFFSET = UNSAFE.getFieldOffset("lastBeatTime", HeartBeatProcessor.class);

    static
    {
        TIMER = new FixedCapacityWheelTimer(1800, new SimpleExpireHandler(), Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), 1, TimeUnit.SECONDS);
    }

    private final        long heartBeatDuration;
    private volatile     long lastBeatTime;

    public HeartBeatProcessor(long heartBeatDuration, TimeUnit unit)
    {
        this.heartBeatDuration = unit.toMillis(heartBeatDuration);
    }

    @Override
    public void bind(final ChannelContext channelContext)
    {
        lastBeatTime = System.currentTimeMillis();
        TIMER.add(new RepeatDelayTrigger(new Timetask()
        {
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
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    canceled = true;
                }
            }
        }, heartBeatDuration, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean process(Object data) throws Throwable
    {
        UNSAFE.putOrderedLong(this, OFFSET, System.currentTimeMillis());
        return downStream.process(data);
    }

    @Override
    public void notifyedWriteAvailable() throws Throwable
    {
        upStream.notifyedWriteAvailable();
    }
}
