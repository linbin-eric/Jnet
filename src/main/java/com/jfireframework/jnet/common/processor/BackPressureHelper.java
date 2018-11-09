package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 放置在有界容量的直接下游前。且直接下游不会暂存数据。
 */
public class BackPressureHelper extends BindDownAndUpStreamDataProcessor<Object>
{
    static final int            transit  = 0;
    static final int            stock    = 1;
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
            flag.set(stock);
            while (downStream.canAccept() && flag.get() == stock)
            {
                if (flag.compareAndSet(stock, transit))
                {
                    if (downStream.process(data))
                    {
                        return true;
                    }
                    else
                    {
                        flag.set(stock);
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
        int now;
        while ((now = flag.get()) == stock)
        {
            Object copy = reSend;
            if (flag.compareAndSet(stock, notifing))
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
                    flag.set(stock);
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
