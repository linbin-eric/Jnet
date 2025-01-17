package com.jfirer.jnet.client;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.internal.ChannelContext;
import com.jfirer.jnet.common.internal.DefaultPipeline;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ClientChannelImpl implements ClientChannel
{
    private volatile ConnectedState            state = ConnectedState.NOT_INIT;
    private InternalPipeline pipeline;
    private ChannelContext   channelContext;
    private ChannelConfig    channelConfig;
    private          ChannelContextInitializer initializer;

    protected ClientChannelImpl(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer   = initializer;
    }

    @Override
    public boolean connect()
    {
        switch (state)
        {
            case NOT_INIT ->
            {
                try
                {
                    AsynchronousSocketChannel asynchronousSocketChannel = AsynchronousSocketChannel.open(channelConfig.getChannelGroup());
                    Future<Void>              future                    = asynchronousSocketChannel.connect(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()));
                    future.get(30, TimeUnit.SECONDS);
                    channelContext = new ChannelContext(asynchronousSocketChannel, channelConfig, DefaultPipeline::new);
                    pipeline       = (InternalPipeline) channelContext.pipeline();
                    pipeline.addReadProcessor(new ReadProcessor<>()
                    {
                        @Override
                        public void channelClose(ReadProcessorNode next, Throwable e)
                        {
                            state = ConnectedState.DISCONNECTED;
                            next.fireChannelClose(e);
                        }

                        @Override
                        public void read(Object data, ReadProcessorNode next)
                        {
                            if (data == null)
                            {
                                System.err.println("数据为空");
                            }
                            next.fireRead(data);
                        }
                    });
                    state = ConnectedState.CONNECTED;
                    initializer.onChannelContextInit(channelContext);
                    pipeline.complete();
                    return true;
                }
                catch (Throwable e)
                {
                    state = ConnectedState.DISCONNECTED;
                    return false;
                }
            }
            case CONNECTED ->
            {
                return true;
            }
            case DISCONNECTED ->
            {
                return false;
            }
            default -> throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    @Override
    public boolean alive()
    {
        return state == ConnectedState.CONNECTED;
    }

    @Override
    public void write(Object data) throws ClosedChannelException
    {
        if (!alive())
        {
            throw new ClosedChannelException();
        }
        pipeline.fireWrite(data);
    }

    @Override
    public ChannelContext channelContext()
    {
        return channelContext;
    }

    @Override
    public void close()
    {
        state = ConnectedState.DISCONNECTED;
        if (channelContext != null)
        {
            channelContext.close();
        }
    }

    @Override
    public void close(Throwable e)
    {
        state = ConnectedState.DISCONNECTED;
        if (channelContext != null)
        {
            channelContext.close(e);
        }
    }
}
