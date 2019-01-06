package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;

public abstract class BindDownAndUpStreamDataProcessor<E> implements DataProcessor<E>
{
    protected DataProcessor  downStream;
    protected DataProcessor  upStream;
    protected ChannelContext channelContext;

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public void bindDownStream(DataProcessor<?> downStream)
    {
        this.downStream = downStream;
    }

    @Override
    public void bindUpStream(DataProcessor<?> upStream)
    {
        this.upStream = upStream;
    }

    @Override
    public boolean canAccept()
    {
        return downStream.canAccept();
    }

    @Override
    public void notifyedWriterAvailable() throws Throwable
    {
        upStream.notifyedWriterAvailable();
    }
}
