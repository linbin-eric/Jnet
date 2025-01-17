package com.jfirer.jnet.client;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.internal.DefaultPipeline;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ClientChannelImpl implements ClientChannel
{
    private volatile ConnectedState      state = ConnectedState.NOT_INIT;
    private          InternalPipeline    pipeline;
    private          ChannelConfig       channelConfig;
    private          PipelineInitializer initializer;

    protected ClientChannelImpl(ChannelConfig channelConfig, PipelineInitializer initializer)
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
                    pipeline = new DefaultPipeline(asynchronousSocketChannel, channelConfig);
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
                    initializer.onPipelineComplete(pipeline);
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
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
