package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.streamprocessor.worker.MutlisAttachWorker;

public class MutliAttachIoProcessor implements ReadProcessor
{
    private final MutlisAttachWorker worker;
    
    public MutliAttachIoProcessor(MutlisAttachWorker worker)
    {
        this.worker = worker;
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        worker.commit(chain, data, channelContext);
    }
    
}
