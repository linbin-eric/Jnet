package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.util.ChannelConfig;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

public class DefaultAcceptHandler implements AcceptHandler
{
    protected final ChannelConfig             channelConfig;
    protected final ChannelContextInitializer channelContextInitializer;
    protected final AioListener               aioListener;

    public DefaultAcceptHandler(ChannelConfig channelConfig, ChannelContextInitializer channelContextInitializer)
    {
        this.channelConfig = channelConfig;
        this.channelContextInitializer = channelContextInitializer;
        aioListener = channelConfig.getAioListener();
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        WriteCompletionHandler writeCompletionHandler = new DefaultWriteCompleteHandler(channelConfig, socketChannel);
        ReadCompletionHandler  readCompletionHandler  = new AdaptiveReadCompletionHandler(channelConfig, socketChannel);
        ChannelContext         channelContext         = new DefaultChannelContext(socketChannel, aioListener, readCompletionHandler, writeCompletionHandler);
        aioListener.onAccept(socketChannel, channelContext);
        channelContextInitializer.onChannelContextInit(channelContext);
        readCompletionHandler.start();
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
