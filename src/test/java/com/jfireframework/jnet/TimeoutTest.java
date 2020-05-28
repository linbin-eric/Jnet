package com.jfireframework.jnet;

import com.jfireframework.jnet.client.DefaultClient;
import com.jfireframework.jnet.client.JnetClient;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.Pipeline;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.server.AioServer;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class TimeoutTest
{
    @Test
    public void test() throws InterruptedException
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setIp("127.0.0.1");
        channelConfig.setPort(8080);
        channelConfig.setReadTimeoutMills(10);
        AioServer aioServer  = new AioServer(channelConfig, new ChannelContextInitializer()
        {
            @Override
            public void onChannelContextInit(Pipeline channelContext)
            {
            }
        });
        aioServer.start();
        ChannelConfig config = new ChannelConfig();
        config.setIp("127.0.0.1");
        config.setPort(8080);
        config.setReadTimeoutMills(5000);
        DefaultClient client  = new DefaultClient(channelConfig, new ChannelContextInitializer()
        {
            @Override
            public void onChannelContextInit(Pipeline channelContext)
            {
            }
        });
        client.connectIfNecessary();
        Thread.sleep(1000);
    }
}
