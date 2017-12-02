package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;

public abstract class ReadProcessorAdapter<T> implements ReadProcessor<T>
{
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        
    }
    
}
