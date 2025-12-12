package cc.jfire.jnet;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;

public class CurrentTest
{
    public static void main(String[] args)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setPort(6000);
        channelConfig.setChannelGroup(ChannelConfig.DEFAULT_CHANNEL_GROUP);
        AioServer aioServer = AioServer.newAioServer(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new ReadProcessor<Object>()
            {
                @Override
                public void read(Object data, ReadProcessorNode next)
                {
                    System.out.println(pipeline.socketChannel());
                    System.out.println("读取到数据");
//                    LockSupport.park();
                }
            });
        });
        aioServer.start();
    }
}
