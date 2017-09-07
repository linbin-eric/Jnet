package com.jfireframework.jnet.common.support;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;

public class DefaultAioListener implements AioListener
{
    
    @Override
    public void afterWrited(ChannelContext channelContext, int writes)
    {
    }
    
    @Override
    public void catchException(Throwable e, ChannelContext channelContext)
    {
        channelContext.close();
    }
    
    @Override
    public void readRegister(ChannelContext channelContext)
    {
    }
    
    @Override
    public void afterReceived(ChannelContext context)
    {
    }
    
}
