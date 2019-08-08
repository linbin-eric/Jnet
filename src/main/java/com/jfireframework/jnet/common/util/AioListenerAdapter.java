package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;

import java.nio.channels.AsynchronousSocketChannel;

public abstract class AioListenerAdapter implements AioListener
{
    @Override
    public void afterWrited(ChannelContext channelContext, Integer writes)
    {
    }

    @Override
    public void onAccept(AsynchronousSocketChannel socketChannel, ChannelContext channelContext)
    {
    }

    @Override
    public void catchException(Throwable e, ChannelContext channelContext)
    {
    }

    @Override
    public void afterReceived(ChannelContext channelContext)
    {
    }

    @Override
    public void onClose(ChannelContext socketChannel, Throwable e)
    {
    }
}
