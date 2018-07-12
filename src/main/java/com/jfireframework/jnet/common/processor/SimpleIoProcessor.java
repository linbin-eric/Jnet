package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;

public class SimpleIoProcessor implements DataProcessor<Object>
{
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        ;
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext) throws Throwable
    {
        chain.chain(data);
    }
    
}
