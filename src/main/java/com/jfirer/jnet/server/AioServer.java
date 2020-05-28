package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.internal.DefaultAcceptHandler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.TimeUnit;

public class AioServer
{
    private ChannelConfig                   channelConfig;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private ChannelContextInitializer       initializer;

    public AioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer = initializer;
    }

    public static AioServer newAioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        return new AioServer(channelConfig, initializer);
    }

    /**
     * 以端口初始化server服务器。
     */
    public void start()
    {
        try
        {
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelConfig.getChannelGroup());
            serverSocketChannel.bind(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()), channelConfig.getBackLog());
            serverSocketChannel.accept(serverSocketChannel, new DefaultAcceptHandler(channelConfig, initializer));
        }
        catch (IOException e)
        {
            ReflectUtil.throwException(e);
        }
    }

    public void shutdown()
    {
        try
        {
            serverSocketChannel.close();
            channelConfig.getChannelGroup().shutdown();
        }
        catch (Exception e)
        {
            ReflectUtil.throwException(e);
        }
    }

    public void termination()
    {
        try
        {
            serverSocketChannel.close();
            channelConfig.getChannelGroup().shutdownNow();
            channelConfig.getChannelGroup().awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }
}
