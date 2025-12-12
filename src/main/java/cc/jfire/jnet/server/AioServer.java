package cc.jfire.jnet.server;

import cc.jfire.jnet.common.api.PipelineInitializer;
import cc.jfire.jnet.common.util.ChannelConfig;

public interface AioServer
{
    static AioServer newAioServer(ChannelConfig channelConfig, PipelineInitializer initializer)
    {
        return new DefaultAioServer(channelConfig, initializer);
    }

    void start();

    void shutdown();

    void termination();
}
