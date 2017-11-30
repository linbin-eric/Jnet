package com.jfireframework.jnet.common.processor;

import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.processor.worker.ChannelAttachWorker;

public class ChannelAttachProcessor implements ReadProcessor
{
    private final ChannelAttachWorker worker;
    
    public ChannelAttachProcessor(ExecutorService executorService)
    {
        worker = new ChannelAttachWorker(executorService);
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        worker.commit(channelContext, chain, data);
    }
    
}
