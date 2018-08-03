package com.jfireframework.jnet.common.processor;

import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.processor.worker.ChannelAttachWorker;

public class ChannelAttachProcessor implements DataProcessor<Object>
{
    private final ChannelAttachWorker worker;
    private ChannelContext            channelContext;
    
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
    public boolean process(Object data, ProcessorInvoker next) throws Throwable
    {
        return worker.commit(channelContext, next, data);
    }
    
}
