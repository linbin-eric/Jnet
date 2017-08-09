package com.jfireframework.jnet.client.client.impl;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.common.channelcontext.ChannelAttachChannelContext;

public class ChannelAttachClient extends AbstractClient
{
    
    public ChannelAttachClient(ExecutorService businessExecutorService, ChannelContextBuilder clientChannelContextBuilder, AsynchronousChannelGroup channelGroup, String serverIp, int port, AioListener aioListener)
    {
        super(businessExecutorService, clientChannelContextBuilder, channelGroup, serverIp, port, aioListener);
    }
    
    @Override
    protected ChannelContext parse(ChannelContextConfig config, AsynchronousSocketChannel socketChannel, AioListener listener)
    {
        ChannelContext serverChannelContext = new ChannelAttachChannelContext(//
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
