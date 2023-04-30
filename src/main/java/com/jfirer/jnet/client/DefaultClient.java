package com.jfirer.jnet.client;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;
import com.jfirer.jnet.common.internal.DefaultChannelContext;
import com.jfirer.jnet.common.internal.DefaultPipeline;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultClient implements JnetClient
{
    enum ConnectedState
    {
        NOT_INIT,
        CONNECTED,
        DISCONNECTED
    }

    private volatile ConnectedState            state = ConnectedState.NOT_INIT;
    private          InternalPipeline          pipeline;
    private          ChannelContext            channelContext;
    private          ChannelConfig             channelConfig;
    private          ChannelContextInitializer initializer;

    public DefaultClient(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer = initializer;
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
                    channelContext = new DefaultChannelContext(asynchronousSocketChannel, channelConfig);
                    pipeline = new DefaultPipeline(channelConfig.getWorkerGroup().next(), channelContext);
                    ((DefaultChannelContext) channelContext).setPipeline(pipeline);
                    initializer.onChannelContextInit(channelContext);
                    pipeline.addReadProcessor(new ReadProcessor<>()
                    {
                        @Override
                        public void channelClose(ReadProcessorNode next)
                        {
                            state = ConnectedState.DISCONNECTED;
                            next.fireChannelClose();
                        }

                        @Override
                        public void read(Object data, ReadProcessorNode next)
                        {
                            next.fireRead(data);
                        }
                    });
                    pipeline.complete();
                    ReadCompletionHandler readCompletionHandler = new AdaptiveReadCompletionHandler(channelContext);
                    readCompletionHandler.start();
                    state = ConnectedState.CONNECTED;
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
            default ->
                    throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    @Override
    public boolean alive()
    {
        return state == ConnectedState.CONNECTED;
    }

    @Override
    public void write(Object data)
    {
        pipeline.fireWrite(data);
    }

    @Override
    public void close()
    {
        state = ConnectedState.DISCONNECTED;
        channelContext.close();
    }
}
