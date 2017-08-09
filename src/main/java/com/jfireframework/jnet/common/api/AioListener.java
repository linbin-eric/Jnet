package com.jfireframework.jnet.common.api;

public interface AioListener
{
    void afterWrited(ChannelContext channelContext, int writes);
    
    void catchException(Throwable e, ChannelContext channelContext);
    
    void readRegister(ChannelContext channelContext);
    
    void afterReceived(ChannelContext context);
    
}
