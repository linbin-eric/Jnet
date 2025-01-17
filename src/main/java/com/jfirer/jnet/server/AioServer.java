package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.PipelineInitializer;
import com.jfirer.jnet.common.util.ChannelConfig;

public interface AioServer
{
    void start();

    void shutdown();

    void termination();

    static AioServer newAioServer(ChannelConfig channelConfig, PipelineInitializer initializer)
    {
        return new DefaultAioServer(channelConfig, initializer);
    }
}
