package com.jfirer.jnet.common.internal.virtual;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.ReadCompletionHandler;
import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;
import com.jfirer.jnet.common.internal.DefaultChannelContext;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class VirtualAcceptHandler   implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConfig             channelConfig;
    protected final ChannelContextInitializer channelContextInitializer;

    public VirtualAcceptHandler(ChannelConfig channelConfig, ChannelContextInitializer channelContextInitializer)
    {
        this.channelConfig = channelConfig;
        this.channelContextInitializer = channelContextInitializer;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        DefaultChannelContext channelContext = new DefaultChannelContext(socketChannel, channelConfig,(channelConfig,context)-> new VirtualThreadPipeline(context));
        channelContextInitializer.onChannelContextInit(channelContext);
        ((InternalPipeline) channelContext.pipeline()).complete();
        ReadCompletionHandler readCompletionHandler = new AdaptiveReadCompletionHandler(channelContext);
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
        catch (Throwable e)
        {
            ;
        }
    }
}
