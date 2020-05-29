package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.api.ChannelContext;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

public class DefaultChannelContext implements ChannelContext
{
    private AsynchronousSocketChannel socketChannel;
    private ChannelConfig             channelConfig;
    private Pipeline                  pipeline;

    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
    }

    @Override
    public ChannelConfig channelConfig()
    {
        return channelConfig;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }

    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }

    @Override
    public void close()
    {
        close(null);
    }

    @Override
    public void close(Throwable e)
    {
        try
        {
            socketChannel.close();
        }
        catch (IOException e1)
        {
            ;
        }
    }

    public void setPipeline(Pipeline pipeline)
    {
        this.pipeline = pipeline;
    }
}
