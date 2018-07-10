package com.jfireframework.jnet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.support.ReadHandler;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConnectListener channelContextBuilder;
    protected final AioListener            aioListener;
    
    public AcceptHandler(ChannelConnectListener channelContextBuilder, AioListener aioListener)
    {
        this.channelContextBuilder = channelContextBuilder;
        this.aioListener = aioListener;
    }
    
    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        ChannelContext channelContext = channelContextBuilder.initChannelContext(socketChannel, aioListener);
        new ReadHandler(aioListener, channelContext).start();
        serverChannel.accept(serverChannel, this);
    }
    
    @Override
    public void failed(Throwable exc, AsynchronousServerSocketChannel serverChannel)
    {
        try
        {
            serverChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
}
