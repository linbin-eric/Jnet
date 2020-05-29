package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConfig             channelConfig;
    protected final ChannelContextInitializer channelContextInitializer;

    public AcceptHandler(ChannelConfig channelConfig, ChannelContextInitializer channelContextInitializer)
    {
        this.channelConfig = channelConfig;
        this.channelContextInitializer = channelContextInitializer;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        DefaultChannelContext channelContext = new DefaultChannelContext(socketChannel, channelConfig);
        DefaultPipeline       pipeline       = new DefaultPipeline(channelConfig.getWorkerGroup(), channelContext);
        pipeline.setChannelContext(channelContext);
        channelContext.setPipeline(pipeline);
        channelContextInitializer.onChannelContextInit(channelContext);
        ReadCompletionHandler readCompletionHandler = new AdaptiveReadCompletionHandler(channelContext);
        pipeline.firePrepareFirstRead();
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
