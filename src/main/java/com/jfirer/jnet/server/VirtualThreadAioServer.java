package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.internal.virtual.VirtualAcceptHandler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.Executors;

public class VirtualThreadAioServer implements AioServer
{
    private ChannelConfig                   channelConfig;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private ChannelContextInitializer       initializer;

    public VirtualThreadAioServer(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer   = initializer;
    }

    /**
     * 以端口初始化server服务器。
     */
    public void start()
    {
        try
        {
            serverSocketChannel = AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withThreadPool(Executors.newVirtualThreadPerTaskExecutor()));
            serverSocketChannel.bind(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()), channelConfig.getBackLog());
            serverSocketChannel.accept(serverSocketChannel, new VirtualAcceptHandler(channelConfig, initializer));
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
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }
}
