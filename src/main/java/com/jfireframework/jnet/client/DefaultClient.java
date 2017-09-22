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
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.StreamProcessor;

public class DefaultClient implements AioClient
{
	private static final int					connectTimeout	= 10;
	protected final AsynchronousChannelGroup	channelGroup;
	protected final String						serverIp;
	protected final int							port;
	protected final AioListener					aioListener;
	protected final ChannelConnectListener		clientChannelContextBuilder;
	protected ChannelContext					clientChannelContext;
	
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
			Configuration config = clientChannelContextBuilder.onConnect(socketChannel, aioListener);
			clientChannelContext = config.config();
			for (StreamProcessor each : clientChannelContext.inProcessors())
			{
				each.initialize(clientChannelContext);
			}
			for (StreamProcessor each : clientChannelContext.outProcessors())
			{
				each.initialize(clientChannelContext);
			}
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
		clientChannelContext.push(packet);
	}
}
