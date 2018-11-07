package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.DataProcessor;

public abstract class BindDownAndUpStreamDataProcessor<E> implements DataProcessor<E>
{
    protected DataProcessor downStream;
    protected DataProcessor upStream;

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
}
