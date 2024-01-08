package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.exception.SelfCloseException;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class DefaultChannelContext extends AtomicInteger implements ChannelContext
{
    private static final int                       OPEN  = 1;
    private static final int                       CLOSE = 0;
    private final        AsynchronousSocketChannel socketChannel;
    private final        ChannelConfig             channelConfig;
    private              InternalPipeline          pipeline;
    private              Object                    attach;

    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig, Function<ChannelContext, InternalPipeline> pipelineGenerator)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
        set(OPEN);
        pipeline = pipelineGenerator.apply(this);
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
        close(new SelfCloseException());
    }

    @Override
    public void close(Throwable e)
    {
        if (!compareAndSet(OPEN, CLOSE))
        {
            return;
        }
        try
        {
            socketChannel.close();
        }
        catch (Throwable ignored)
        {
            ;
        }
    }

    @Override
    public void setAttach(Object attach)
    {
        this.attach = attach;
    }

    @Override
    public Object getAttach()
    {
        return attach;
    }

    public void setPipeline(InternalPipeline pipeline)
    {
        this.pipeline = pipeline;
    }
}
