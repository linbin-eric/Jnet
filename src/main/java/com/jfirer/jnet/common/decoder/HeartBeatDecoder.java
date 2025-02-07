package com.jfirer.jnet.common.decoder;

import com.jfirer.baseutil.schedule.timer.FixedCapacityWheelTimer;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartBeatDecoder implements ReadProcessor
{
    private static final FixedCapacityWheelTimer TIMER = new FixedCapacityWheelTimer(60 * 60, null, Executors.newCachedThreadPool(), 1, TimeUnit.SECONDS);

    public HeartBeatDecoder(int secondOfTimeout)
    {
        TIMER.add();
    }

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        next.fireRead(data);
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        next.fireReadFailed(e);
    }
}
