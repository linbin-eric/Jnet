package com.jfirer.jnet;

import com.jfirer.jnet.client.DefaultClient;
import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;
import org.junit.Test;

public class TimeoutTest
{
    @Test
    public void test() throws InterruptedException
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setIp("127.0.0.1");
        channelConfig.setPort(8080);
        AioServer aioServer = new AioServer(channelConfig, new ChannelContextInitializer()
        {
            @Override
            public void onChannelContextInit(ChannelContext channelContext)
            {
            }
        });
        aioServer.start();
        ChannelConfig config = new ChannelConfig();
        config.setIp("127.0.0.1");
        config.setPort(8080);
        DefaultClient client = new DefaultClient(channelConfig, new ChannelContextInitializer()
        {
            @Override
            public void onChannelContextInit(ChannelContext channelContext)
            {
            }
        });
        client.connectIfNecessary();
        Thread.sleep(1000);
    }
}
