package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.api.ChannelContext;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultChannelContext extends AtomicInteger implements ChannelContext
{
    private static final int                       OPEN  = 1;
    private static final int                       CLOSE = 0;
    private              AsynchronousSocketChannel socketChannel;
    private              ChannelConfig             channelConfig;
    private              Pipeline                  pipeline;

    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
        set(OPEN);
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
        if (compareAndSet(OPEN,CLOSE)==false)
        {
            return;
        }
        try
        {
            socketChannel.close();
        }
        catch (IOException e1)
        {
            ;
        }
        pipeline.fireChannelClose();
    }

    public void setPipeline(Pipeline pipeline)
    {
        this.pipeline = pipeline;
    }
}
