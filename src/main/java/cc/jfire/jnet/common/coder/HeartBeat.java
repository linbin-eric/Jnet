package cc.jfire.jnet.common.coder;

import cc.jfire.baseutil.schedule.timer.SimpleWheelTimer;
import cc.jfire.baseutil.schedule.trigger.RepeatDelayTrigger;
import cc.jfire.jnet.common.api.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartBeat implements ReadProcessor, WriteProcessor
{
    private static final SimpleWheelTimer timer       = new SimpleWheelTimer(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), 1000);
    private final        long             heartBeatDuration;
    private final        Pipeline         pipeline;
    private volatile     long             prevTime;
    private volatile     boolean          noNeedWatch = false;

    public HeartBeat(int secondOfTimeout, Pipeline pipeline)
    {
        this.pipeline     = pipeline;
        prevTime          = System.currentTimeMillis();
        heartBeatDuration = TimeUnit.SECONDS.toMillis(secondOfTimeout);
        timer.add(new CheckTrigger(secondOfTimeout, TimeUnit.SECONDS));
    }

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        prevTime = System.currentTimeMillis();
        next.fireRead(data);
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        noNeedWatch = true;
        next.fireReadFailed(e);
    }

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        prevTime = System.currentTimeMillis();
        next.fireWrite(data);
    }

    @Override
    public void channelClosed(WriteProcessorNode next, Throwable e)
    {
        noNeedWatch = true;
        next.fireChannelClosed(e);
    }

    class CheckTrigger extends RepeatDelayTrigger
    {
        public CheckTrigger(long delay, TimeUnit unit)
        {
            super(() -> {
                if (System.currentTimeMillis() - prevTime > heartBeatDuration)
                {
                    noNeedWatch = true;
                    pipeline.shutdownInput();
                }
                else
                {
                    ;
                }
            }, delay, unit);
        }

        @Override
        public boolean calNext()
        {
            if (noNeedWatch || (System.currentTimeMillis() - prevTime) > heartBeatDuration)
            {
                return false;
            }
            else
            {
                return super.calNext();
            }
        }
    }
}
