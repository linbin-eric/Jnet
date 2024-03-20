package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.ReadCompletionHandler;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConfig             channelConfig;
    protected final ChannelContextInitializer channelContextInitializer;

    public AcceptHandler(ChannelConfig channelConfig, ChannelContextInitializer channelContextInitializer)
    {
        this.channelConfig             = channelConfig;
        this.channelContextInitializer = channelContextInitializer;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        DefaultChannelContext channelContext = new DefaultChannelContext(socketChannel, channelConfig, DefaultPipeline::new);
        channelContextInitializer.onChannelContextInit(channelContext);
        ((InternalPipeline) channelContext.pipeline()).complete();
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
