package com.jfirer.jnet;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.DefaultAioServer;
import org.junit.Test;

public class TimeoutTest
{
    @Test
    public void test() throws InterruptedException
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setWorkerGroup(new DefaultWorkerGroup(2, "timeout_"));
        channelConfig.setChannelGroup(ChannelConfig.DEFAULT_CHANNEL_GROUP);
        channelConfig.setIp("127.0.0.1");
        channelConfig.setPort(8080);
        DefaultAioServer aioServer = new DefaultAioServer(channelConfig, channelContext ->
        {
        });
        aioServer.start();
        ChannelConfig config = new ChannelConfig();
        config.setIp("127.0.0.1");
        config.setPort(8080);
        ClientChannel client = ClientChannel.newClient(channelConfig, channelContext ->
        {
        });
        client.connect();
        Thread.sleep(1000);
    }
}
