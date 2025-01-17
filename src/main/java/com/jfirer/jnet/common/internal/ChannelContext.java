package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.exception.SelfCloseException;
import com.jfirer.jnet.common.util.ChannelConfig;
import lombok.Data;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Data
public class ChannelContext extends AtomicInteger
{
    private static final int                       OPEN  = 1;
    private static final int                       CLOSE = 0;
    private final        AsynchronousSocketChannel socketChannel;
    private final        ChannelConfig             channelConfig;
    private final        InternalPipeline          pipeline;
    private              Object                    attach;

    public ChannelContext(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig, Function<ChannelContext, InternalPipeline> pipelineGenerator)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
        set(OPEN);
        pipeline = pipelineGenerator.apply(this);
    }

    public ChannelConfig channelConfig()
    {
        return channelConfig;
    }

    public Pipeline pipeline()
    {
        return pipeline;
    }

    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }

    public void close()
    {
        close(new SelfCloseException());
    }

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

    public String getRemoteAddressWithoutException()
    {
        try
        {
            return socketChannel.getRemoteAddress().toString();
        }
        catch (Throwable e)
        {
            return null;
        }
    }
}
