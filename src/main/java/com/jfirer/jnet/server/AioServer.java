package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.util.ChannelConfig;

public interface AioServer
{
    void start();

    void shutdown();

    void termination();

    static AioServer newAioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer, boolean useVirtualThread)
    {
        return useVirtualThread ? new VirtualThreadAioServer(channelConfig, initializer) : new DefaultAioServer(channelConfig, initializer);
    }

    static AioServer newAioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        return newAioServer(channelConfig, initializer, false);
    }
}
