package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.processor.worker.MutlisAttachWorker;

public class MutliAttachIoProcessor implements DataProcessor<Object>
{
    private final MutlisAttachWorker worker;
    
    public MutliAttachIoProcessor(MutlisAttachWorker worker)
    {
        this.worker = worker;
    }
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        worker.commit(chain, data, channelContext);
    }
    
}
