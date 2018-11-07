package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.processor.worker.ChannelAttachWorker;

import java.util.concurrent.ExecutorService;

public class ChannelAttachProcessor extends BindDownAndUpStreamDataProcessor<Object>
{
    private final ChannelAttachWorker worker;
    private       ChannelContext      channelContext;

    public ChannelAttachProcessor(ExecutorService executorService)
    {
        worker = new ChannelAttachWorker(executorService);
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(Object data) throws Throwable
    {
        return worker.commit(channelContext, downStream, data);
    }

    @Override
    public void notifyedWriteAvailable() throws Throwable
    {
        worker.notifyedWriteAvailable();
    }

    @Override
    public void bindUpStream(DataProcessor<?> upStream)
    {
        this.upStream = upStream;
        worker.setUpStream(upStream);
    }

    @Override
    public boolean canAccept()
    {
        return worker.canAccept();
    }
}
