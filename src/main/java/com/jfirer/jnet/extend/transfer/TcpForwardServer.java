package com.jfirer.jnet.extend.transfer;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;

import java.nio.channels.ClosedChannelException;

public class TcpForwardServer
{
    static boolean useVirtualThread = Integer.parseInt(System.getProperty("java.specification.version")) >= 21;

    public static void start(String localIp, int localPort, String destIp, int destPort)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setIp(localIp);
        channelConfig.setPort(localPort);
        channelConfig.setWorkerGroup(ChannelConfig.DEFAULT_WORKER_GROUP);
        channelConfig.setChannelGroup(ChannelConfig.DEFAULT_CHANNEL_GROUP);
        ChannelConfig remoteConfig = new ChannelConfig();
        remoteConfig.setIp(destIp);
        remoteConfig.setPort(destPort);
        remoteConfig.setChannelGroup(ChannelConfig.DEFAULT_CHANNEL_GROUP);
        remoteConfig.setWorkerGroup(ChannelConfig.DEFAULT_WORKER_GROUP);
        AioServer aioServer = AioServer.newAioServer(channelConfig, channelContext -> {
            Pipeline pipeline = channelContext.pipeline();
            pipeline.addReadProcessor(new TcpForwardHandler(remoteConfig));
        });
        aioServer.start();
    }

    private static class TcpForwardHandler implements ReadProcessor<IoBuffer>
    {
        private final ChannelConfig channelConfig;
        private       ClientChannel remoteChannel;

        public TcpForwardHandler(ChannelConfig channelConfig)
        {
            this.channelConfig = channelConfig;
        }

        @Override
        public void read(IoBuffer ioBuffer, ReadProcessorNode readProcessorNode)
        {
            try
            {
                remoteChannel.write(ioBuffer);
            }
            catch (ClosedChannelException e)
            {
                readProcessorNode.pipeline().channelContext().close();
            }
        }

        @Override
        public void pipelineComplete(ReadProcessorNode next)
        {
            Pipeline localPipeline = next.pipeline();
            remoteChannel = ClientChannel.newClient(channelConfig, new RemoteIniter(localPipeline));
            if (remoteChannel.connect())
            {
                ;
            }
            else
            {
                localPipeline.channelContext().close();
            }
        }

        @Override
        public void channelClose(ReadProcessorNode next, Throwable e)
        {
            next.fireChannelClose(e);
            remoteChannel.close();
        }
    }

    static class RemoteIniter implements ChannelContextInitializer
    {
        Pipeline localPipeline;

        public RemoteIniter(Pipeline localPipeline)
        {
            this.localPipeline = localPipeline;
        }

        @Override
        public void onChannelContextInit(ChannelContext channelContext)
        {
            channelContext.pipeline().addReadProcessor(new ReadProcessor<IoBuffer>()
            {
                @Override
                public void read(IoBuffer data, ReadProcessorNode next)
                {
                    try
                    {
                        localPipeline.fireWrite(data);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void channelClose(ReadProcessorNode next, Throwable e)
                {
                    localPipeline.channelContext().close();
                }
            });
        }
    }

    public static void main(String[] args)
    {
        TcpForwardServer.start("127.0.0.1", 3360, "localhost", 3306);
    }
}
