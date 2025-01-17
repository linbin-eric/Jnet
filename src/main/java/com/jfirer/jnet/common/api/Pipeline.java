package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.internal.ChannelContext;

public interface Pipeline
{
    void fireWrite(Object data);

    void addReadProcessor(ReadProcessor<?> processor);

    void addWriteProcessor(WriteProcessor<?> processor);

    ChannelContext channelContext();

    static void invokeMethodIgnoreException(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable e)
        {
            ;
        }
    }
}
