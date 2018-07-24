package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;

public class ThreadYieldRetryProcessor implements DataProcessor<Object>
{
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        
    }
    
    @Override
    public boolean process(Object data, ProcessorInvoker next) throws Throwable
    {
        while (next.process(data) == false)
        {
            Thread.yield();
        }
        return true;
    }
    
}
