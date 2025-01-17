package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.PipelineInitializer;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final ChannelConfig       channelConfig;
    protected final PipelineInitializer pipelineInitializer;

    public AcceptHandler(ChannelConfig channelConfig, PipelineInitializer pipelineInitializer)
    {
        this.channelConfig       = channelConfig;
        this.pipelineInitializer = pipelineInitializer;
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        DefaultPipeline pipeline = new DefaultPipeline(socketChannel, channelConfig);
        pipelineInitializer.onPipelineComplete(pipeline);
        pipeline.complete();
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
