package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.processor.worker.FixedAttachWorker;

public class FixedAttachIoProcessor extends BindDownAndUpStreamDataProcessor<Object>
{
    private final FixedAttachWorker worker;
    private       ChannelContext    channelContext;

    public FixedAttachIoProcessor(FixedAttachWorker worker)
    {
        this.worker = worker;
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(Object data) throws Throwable
    {
//        worker.commit(channelContext, next, data);
        return true;
    }

    @Override
    public void notifyedWriteAvailable() throws Throwable
    {
    }
}
