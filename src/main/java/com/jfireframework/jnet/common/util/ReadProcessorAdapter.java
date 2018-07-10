package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;

public abstract class ReadProcessorAdapter<T> implements DataProcessor<T>
{
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        
    }
    
}
