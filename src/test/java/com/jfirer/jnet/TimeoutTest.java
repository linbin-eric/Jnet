package com.jfirer.jnet;

import com.jfirer.jnet.client.ClientChannelImpl;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;
import org.junit.Test;

public class TimeoutTest
{
    @Test
    public void test() throws InterruptedException
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setWorkerGroup(new DefaultWorkerGroup());
        channelConfig.setIp("127.0.0.1");
        channelConfig.setPort(8080);
        AioServer aioServer = new AioServer(channelConfig, channelContext -> {});
        aioServer.start();
        ChannelConfig config = new ChannelConfig();
        config.setIp("127.0.0.1");
        config.setPort(8080);
        ClientChannelImpl client = new ClientChannelImpl(channelConfig, channelContext -> {});
        client.connect();
        Thread.sleep(1000);
    }
}
