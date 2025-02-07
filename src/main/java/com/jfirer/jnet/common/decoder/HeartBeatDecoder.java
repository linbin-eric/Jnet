package com.jfirer.jnet.common.decoder;

import com.jfirer.baseutil.schedule.timer.SimpleWheelTimer;
import com.jfirer.baseutil.schedule.trigger.RepeatDelayTrigger;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartBeatDecoder implements ReadProcessor
{
    private static final SimpleWheelTimer timer       = new SimpleWheelTimer(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), 1000);
    private final        long             heartBeatDuration;
    private final        Pipeline         pipeline;
    private volatile     long             prevTime;
    private volatile     boolean          noNeedWatch = false;

    public HeartBeatDecoder(int secondOfTimeout, Pipeline pipeline)
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
