package com.jfireframework.jnet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;

public class DefaultClient implements AioClient
{
    private static final int                 connectTimeout = 10;
    protected final AsynchronousChannelGroup channelGroup;
    protected final String                   serverIp;
    protected final int                      port;
    protected final AioListener              aioListener;
    protected final ChannelConnectListener   clientChannelContextBuilder;
    protected ChannelContext                 channelContext;
    
    public DefaultClient(ChannelConnectListener clientChannelContextBuilder, AsynchronousChannelGroup channelGroup, String serverIp, int port, AioListener aioListener)
    {
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
            channelContext = clientChannelContextBuilder.initChannelContext(socketChannel, aioListener);
            new DefaultReadCompletionHandler(aioListener, channelContext).start();
        }
        catch (IOException | InterruptedException | ExecutionException | TimeoutException e)
        {
            throw new JustThrowException(e);
        }
    }
    
    @Override
    public void close()
    {
        try
        {
            channelContext.socketChannel().close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    @Override
    public void write(PooledIoBuffer packet)
    {
        channelContext.write(packet);
    }
}
