package com.jfirer.jnet.client;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;
import com.jfirer.jnet.common.internal.DefaultChannelContext;
import com.jfirer.jnet.common.internal.DefaultPipeline;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class DefaultClient implements JnetClient
{
    private static final int              NOT_INIT     = 1;
    private static final int              CONNECTED    = 2;
    private static final int              DISCONNECTED = 3;
    private volatile     int              state        = NOT_INIT;
    private              InternalPipeline pipeline;
    private              ChannelContext   channelContext;

    @Override
    public boolean connect(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        switch (state)
        {
            case NOT_INIT ->
            {
                try
                {
                    AsynchronousSocketChannel asynchronousSocketChannel = AsynchronousSocketChannel.open(channelConfig.getChannelGroup());
                    Future<Void>              future                    = asynchronousSocketChannel.connect(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()));
                    future.get();
                    channelContext = new DefaultChannelContext(asynchronousSocketChannel, channelConfig);
                    pipeline = new DefaultPipeline(channelConfig.getWorkerGroup().next(), channelContext);
                    ((DefaultChannelContext) channelContext).setPipeline(pipeline);
                    initializer.onChannelContextInit(channelContext);
                    pipeline.addReadProcessor(new ReadProcessor<Object>()
                    {
                        @Override
                        public void channelClose(ReadProcessorNode next)
                        {
                            state = DISCONNECTED;
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
                    state = CONNECTED;
                    return true;
                }
                catch (Throwable e)
                {
                    state = DISCONNECTED;
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
        return state == CONNECTED;
    }

    @Override
    public void asyncWrite(Object data)
    {
        pipeline.fireWrite(data);
    }

    @Override
    public void close()
    {
        channelContext.close();
        state = DISCONNECTED;
    }
}
