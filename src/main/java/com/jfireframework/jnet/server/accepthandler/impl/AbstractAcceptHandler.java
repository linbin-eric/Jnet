package com.jfireframework.jnet.server.accepthandler.impl;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.server.accepthandler.AcceptHandler;

public abstract class AbstractAcceptHandler implements AcceptHandler
{
    protected final ChannelContextBuilder           channelContextBuilder;
    protected final AsynchronousServerSocketChannel serverSocketChannel;
    protected final AioListener                     serverListener;
    protected final ExecutorService                 businessExecutorService;
    
    public AbstractAcceptHandler(ExecutorService businessExecutorService, AsynchronousServerSocketChannel serverSocketChannel, ChannelContextBuilder channelContextBuilder, AioListener serverListener)
    {
        this.channelContextBuilder = channelContextBuilder;
        this.serverSocketChannel = serverSocketChannel;
        this.serverListener = serverListener;
        this.businessExecutorService = businessExecutorService;
    }
    
    @Override
    public void completed(AsynchronousSocketChannel socketChannel, Object attachment)
    {
        ChannelContextConfig config = channelContextBuilder.onConnect(socketChannel);
        ChannelContext serverChannelContext = parse(businessExecutorService, socketChannel, config, serverListener);
        channelContextBuilder.afterContextBuild(serverChannelContext);
        serverChannelContext.registerRead();
        serverSocketChannel.accept(null, this);
    }
    
    protected abstract ChannelContext parse(ExecutorService businessExecutorService, AsynchronousSocketChannel socketChannel, ChannelContextConfig config, AioListener listener);
    
    @Override
    public void stop()
    {
        if (businessExecutorService != null)
        {
            businessExecutorService.shutdown();
        }
    }
    
    @Override
    public void failed(Throwable exc, Object attachment)
    {
        try
        {
            serverSocketChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
}
