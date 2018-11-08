package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

import java.util.concurrent.atomic.AtomicInteger;

public class BackPressureHelper extends BindDownAndUpStreamDataProcessor<Object>
{
    static final int            transit  = 0;
    static final int            locker   = 1;
    static final int            notifing = 2;
    protected    ChannelContext channelContext;

    Object        reSend;
    AtomicInteger flag = new AtomicInteger(transit);

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
            flag.set(locker);
            while (downStream.canAccept() && flag.get() == locker)
            {
                if (flag.compareAndSet(locker, transit))
                {
                    if (downStream.process(data))
                    {
                        return true;
                    }
                    else
                    {
                        flag.set(locker);
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
    public void notifyedWriteAvailable() throws Throwable
    {
        int now = flag.get();
        while (downStream.canAccept() && now == locker)
        {
            Object copy = reSend;
            if (flag.compareAndSet(locker, notifing))
            {
//                System.out.println(Thread.currentThread().getName()+"处理暂存数据");
                if (downStream.process(copy))
                {
                    flag.set(transit);
//                    System.out.println(Thread.currentThread().getName()+"继续通知上游");
                    upStream.notifyedWriteAvailable();
                    return;
                }
                else
                {
                    flag.set(locker);
                }
            }
            else
            {
                break;
            }
        }
//        System.out.println(Thread.currentThread().getName()+"当前暂存状态："+now);
    }

    @Override
    public boolean canAccept()
    {
        if (flag.get() == transit)
        {
            return true;
        }
//        System.out.println(Thread.currentThread().getName()+"backpressure返回无法接受");
        return false;
    }
}
