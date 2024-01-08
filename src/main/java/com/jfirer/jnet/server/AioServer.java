package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.util.ChannelConfig;

public interface AioServer
{
    void start();

    void shutdown();

    void termination();

    static AioServer newAioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        return new DefaultAioServer(channelConfig, initializer);
    }
}
