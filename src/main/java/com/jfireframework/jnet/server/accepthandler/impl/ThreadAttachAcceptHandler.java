package com.jfireframework.jnet.server.accepthandler.impl;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.common.channelcontext.ThreadAttachChannelContext;

public class ThreadAttachAcceptHandler extends AbstractAcceptHandler
{
    
    public ThreadAttachAcceptHandler(ExecutorService businessExecutorService, AsynchronousServerSocketChannel serverSocketChannel, ChannelContextBuilder channelContextBuilder, AioListener serverListener)
    {
        super(businessExecutorService, serverSocketChannel, channelContextBuilder, serverListener);
    }
    
    @Override
    protected ChannelContext parse(ExecutorService businessExecutorService, AsynchronousSocketChannel socketChannel, ChannelContextConfig config, AioListener listener)
    {
        ChannelContext serverChannelContext = new ThreadAttachChannelContext(//
                businessExecutorService, //
                config.getBufStorage(), //
                config.getMaxMerge(), //
                listener, //
                config.getInProcessors(), //
                config.getOutProcessors(), //
                socketChannel, //
                config.getFrameDecodec());
        return serverChannelContext;
    }
    
}
