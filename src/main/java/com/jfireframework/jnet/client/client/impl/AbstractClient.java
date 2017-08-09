package com.jfireframework.jnet.client.client.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.client.client.AioClient;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.build.ChannelContextConfig;

public abstract class AbstractClient implements AioClient
{
    private static final int                 connectTimeout = 10;
    protected final AsynchronousChannelGroup channelGroup;
    protected final String                   serverIp;
    protected final int                      port;
    protected final AioListener              aioListener;
    protected final ChannelContextBuilder    clientChannelContextBuilder;
    protected ChannelContext                 clientChannelContext;
    protected final ExecutorService          businessExecutorService;
    
    public AbstractClient(ExecutorService businessExecutorService, ChannelContextBuilder clientChannelContextBuilder, AsynchronousChannelGroup channelGroup, String serverIp, int port, AioListener aioListener)
    {
        this.businessExecutorService = businessExecutorService;
        this.channelGroup = channelGroup;
        this.serverIp = serverIp;
        this.port = port;
        this.aioListener = aioListener;
        this.clientChannelContextBuilder = clientChannelContextBuilder;
    }
    
    @Override
    public void connect()
    {
        try
        {
            AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(channelGroup);
            socketChannel.connect(new InetSocketAddress(serverIp, port)).get(connectTimeout, TimeUnit.SECONDS);
            ChannelContextConfig config = clientChannelContextBuilder.onConnect(socketChannel);
            clientChannelContext = parse(config, socketChannel, aioListener);
            clientChannelContext.registerRead();
        }
        catch (IOException | InterruptedException | ExecutionException | TimeoutException e)
        {
            throw new JustThrowException(e);
        }
    }
    
    protected abstract ChannelContext parse(ChannelContextConfig config, AsynchronousSocketChannel socketChannel, AioListener listener);
    
    @Override
    public void close()
    {
        clientChannelContext.close();
        clientChannelContext = null;
    }
    
    @Override
    public void write(Object packet) throws Throwable
    {
        clientChannelContext.push(packet, 0);
    }
}
