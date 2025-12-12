package cc.jfire.jnet.client;

import cc.jfire.jnet.common.api.*;
import cc.jfire.jnet.common.internal.DefaultPipeline;
import cc.jfire.jnet.common.util.ChannelConfig;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientChannelImpl implements ClientChannel
{
    private volatile ConnectedState      state = ConnectedState.NOT_INIT;
    private          InternalPipeline    pipeline;
    private final    ChannelConfig       channelConfig;
    private final    PipelineInitializer initializer;
    @Getter
    private          Throwable           connectionException;

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
                    ConnectedResult           connectedResult           = new ConnectedResult(1);
                    asynchronousSocketChannel.connect(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()), connectedResult, new CompletionHandler<>()
                    {
                        @Override
                        public void completed(Void result, ConnectedResult attachment)
                        {
                            attachment.setSuccess(true);
                            attachment.countDown();
                        }

                        @Override
                        public void failed(Throwable exc, ConnectedResult attachment)
                        {
                            attachment.setE(exc);
                            attachment.setSuccess(false);
                            attachment.countDown();
                        }
                    });
                    if (connectedResult.await(30, TimeUnit.SECONDS))
                    {
                        if (connectedResult.isSuccess())
                        {
                            pipeline = new DefaultPipeline(asynchronousSocketChannel, channelConfig);
                            pipeline.addReadProcessor(new ReadProcessor<>()
                            {
                                @Override
                                public void readFailed(Throwable e, ReadProcessorNode next)
                                {
                                    state = ConnectedState.DISCONNECTED;
                                    next.fireReadFailed(e);
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
                        else
                        {
                            connectionException = connectedResult.getE();
                            state               = ConnectedState.DISCONNECTED;
                            return false;
                        }
                    }
                    else
                    {
                        state = ConnectedState.DISCONNECTED;
                        return false;
                    }
                }
                catch (Throwable e)
                {
                    state               = ConnectedState.DISCONNECTED;
                    connectionException = e;
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

    @Setter
    @Getter
    class ConnectedResult extends CountDownLatch
    {
        boolean   success;
        Throwable e;

        /**
         * Constructs a {@code CountDownLatch} initialized with the given count.
         *
         * @param count the number of times {@link #countDown} must be invoked
         *              before threads can pass through {@link #await}
         * @throws IllegalArgumentException if {@code count} is negative
         */
        public ConnectedResult(int count)
        {
            super(count);
        }
    }
}
