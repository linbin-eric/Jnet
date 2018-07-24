package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;

public class BackPressureSubmitProcessor implements DataProcessor<Object>
{
    private ChannelContext channelContext;
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }
    
    @Override
    public boolean process(Object data, ProcessorInvoker next) throws Throwable
    {
        if (next.process(data) == false)
        {
            channelContext.submitBackPressureTask(next, data);
            return false;
        }
        else
        {
            return true;
        }
    }
    
}
