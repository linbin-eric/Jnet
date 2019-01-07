package com.jfireframework.jnet.common.processor;

import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 放置在有界容量的直接下游前。且直接下游不会暂存数据。
 */
public class BackPressureHelper extends BindDownAndUpStreamDataProcessor<Object>
{
    static final int            TRANSIT  = 0;
    static final int            STOCK    = 1;
    static final int            NOTIFING = 2;
    protected    ChannelContext channelContext;
    Object        reSend;
    volatile     int  state     = TRANSIT;
    static final long STATE_OFF = UNSAFE.getFieldOffset("state", BackPressureHelper.class);

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(Object data) throws Throwable
    {
        if (downStream.process(data))
        {
            return true;
        }
        else
        {
            reSend = data;
            state = STOCK;
            while (downStream.canAccept() && state == STOCK)
            {
                if(UNSAFE.compareAndSwapInt(this,STATE_OFF,STOCK,TRANSIT))
                {
                    if (downStream.process(data))
                    {
                        return true;
                    }
                    else
                    {
                        state = STOCK;
                    }
                }
                else
                {
                    break;
                }
            }
            return false;
        }
    }

    @Override
    public void notifyedWriterAvailable() throws Throwable
    {
        int now;
        while ((now = state) == STOCK)
        {
            Object copy = reSend;
            if(UNSAFE.compareAndSwapInt(this,STATE_OFF,STOCK,NOTIFING))
            {
                if (downStream.process(copy))
                {
                    state = TRANSIT;
                    upStream.notifyedWriterAvailable();
                    return;
                }
                else
                {
                    state = STOCK;
                    if (downStream.canAccept() == false)
                    {
                        break;
                    }
                }
            }
            else
            {
                break;
            }
        }
    }

    @Override
    public boolean canAccept()
    {
        if(state ==TRANSIT)
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isBoundary()
    {
        return false;
    }
}
