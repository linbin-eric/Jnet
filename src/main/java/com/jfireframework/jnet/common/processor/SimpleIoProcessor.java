package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;

public class SimpleIoProcessor implements ReadProcessor
{
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        ;
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        chain.chain(data);
    }
    
}
