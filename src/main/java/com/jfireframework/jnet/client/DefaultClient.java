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
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextBuilder;
import com.jfireframework.jnet.common.api.Configuration;

public class DefaultClient implements AioClient
{
	private static final int					connectTimeout	= 10;
	protected final AsynchronousChannelGroup	channelGroup;
	protected final String						serverIp;
	protected final int							port;
	protected final AioListener					aioListener;
	protected final ChannelContextBuilder		clientChannelContextBuilder;
	protected ChannelContext					clientChannelContext;
	
	public DefaultClient(ChannelContextBuilder clientChannelContextBuilder, AsynchronousChannelGroup channelGroup, String serverIp, int port, AioListener aioListener)
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
			Configuration config = clientChannelContextBuilder.onConnect(socketChannel, aioListener);
			clientChannelContext = config.config();
			clientChannelContext.registerRead();
		}
		catch (IOException | InterruptedException | ExecutionException | TimeoutException e)
		{
			throw new JustThrowException(e);
		}
	}
	
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
