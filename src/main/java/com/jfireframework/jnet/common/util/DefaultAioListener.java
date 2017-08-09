package com.jfireframework.jnet.common.util;

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
//        e.printStackTrace();
        channelContext.close();
    }
    
    @Override
    public void readRegister(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void afterReceived(ChannelContext context)
    {
        // TODO Auto-generated method stub
        
    }
    
}
