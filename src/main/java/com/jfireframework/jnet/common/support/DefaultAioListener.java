package com.jfireframework.jnet.common.support;

import java.io.IOException;
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
        try
        {
            channelContext.socketChannel().close();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
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
