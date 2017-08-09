package com.jfireframework.jnet.client.client.impl;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;
import com.jfireframework.jnet.common.businessprocessor.MutlisAttachProcessor;
import com.jfireframework.jnet.common.channelcontext.MutliAttachChannelContext;

public class MutliAttachClient extends AbstractClient
{
    private MutlisAttachProcessor[] processors;
    private int                     index = 0;
    private final int               mask;
    
    public MutliAttachClient(MutlisAttachProcessor[] processors, ExecutorService businessExecutorService, ChannelContextBuilder clientChannelContextBuilder, AsynchronousChannelGroup channelGroup, String serverIp, int port, AioListener aioListener)
    {
        super(businessExecutorService, clientChannelContextBuilder, channelGroup, serverIp, port, aioListener);
        this.processors = processors;
        mask = processors.length - 1;
    }
    
    @Override
    protected ChannelContext parse(ChannelContextConfig config, AsynchronousSocketChannel socketChannel, AioListener listener)
    {
        MutlisAttachProcessor processor = processors[(int) (index & mask)];
        index += 1;
        ChannelContext serverChannelContext = new MutliAttachChannelContext(//
                processor, //
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
