package com.jfirer.jnet.server;

import com.jfirer.jnet.common.api.PipelineInitializer;
import com.jfirer.jnet.common.internal.AcceptHandler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

public class DefaultAioServer implements AioServer
{
    private final ChannelConfig                   channelConfig;
    private       AsynchronousServerSocketChannel serverSocketChannel;
    private final PipelineInitializer             initializer;

    public DefaultAioServer(ChannelConfig channelConfig, PipelineInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer   = initializer;
    }

    /**
     * 以端口初始化server服务器。
     */
    @Override
    public void start()
    {
        try
        {
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelConfig.getChannelGroup());
            serverSocketChannel.bind(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()), channelConfig.getBackLog());
            serverSocketChannel.accept(serverSocketChannel, new AcceptHandler(channelConfig, initializer));
        }
        catch (IOException e)
        {
            ReflectUtil.throwException(e);
        }
    }

    @Override
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

    @Override
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
