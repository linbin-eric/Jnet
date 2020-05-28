package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.ChannelConfig;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

public class DefaultChannelContext implements ChannelContext
{
    private AsynchronousSocketChannel socketChannel;
    private AioListener               aioListener;
    private ChannelConfig             channelConfig;

    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, AioListener aioListener, ChannelConfig channelConfig)
    {
        this.socketChannel = socketChannel;
        this.aioListener = aioListener;
        this.channelConfig = channelConfig;
    }

    @Override
    public ChannelConfig channelConfig()
    {
        return channelConfig;
    }

    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }

    @Override
    public void close()
    {
        close(null);
    }

    @Override
    public void close(Throwable e)
    {
        try
        {
            socketChannel.close();
            aioListener.onClose(this, e);
        }
        catch (IOException e1)
        {
            ;
        }
    }
}
