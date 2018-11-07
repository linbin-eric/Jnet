package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

import java.util.concurrent.atomic.AtomicInteger;

public class BackPressureHelper extends BindDownAndUpStreamDataProcessor<Object>
{
    protected ChannelContext channelContext;

    Object        reSend;
    AtomicInteger flag = new AtomicInteger(0);

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(Object data) throws Throwable
    {
        if (downStream.process(data) == false)
        {
            reSend = data;
            flag.set(1);
            while (downStream.canAccept() && flag.get() == 1)
            {
                if (flag.compareAndSet(1, 0))
                {
                    if (downStream.process(data))
                    {
                        return true;
                    }
                    else
                    {
                        flag.set(1);
                    }
                }
            }
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void notifyedWriteAvailable() throws Throwable
    {
        while (downStream.canAccept() && flag.get() == 1)
        {
            Object copy = reSend;
            if (flag.compareAndSet(1, 2))
            {
                if (downStream.process(copy))
                {
                    flag.set(0);
                    upStream.notifyedWriteAvailable();
                    break;
                }
                else
                {
                    flag.set(1);
                }
            }
        }
    }

    @Override
    public boolean canAccept()
    {
        if (flag.get() == 0)
        {
            return downStream.canAccept();
        }
        return false;
    }
}
