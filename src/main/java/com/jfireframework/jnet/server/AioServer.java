package com.jfireframework.jnet.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.TimeUnit;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.server.accepthandler.impl.AcceptHandler;

public class AioServer
{
	private AsynchronousChannelGroup		channelGroup;
	private AsynchronousServerSocketChannel	serverSocketChannel;
	private String							ip;
	private int								port;
	private AcceptHandler					acceptHandler;
	
	public AioServer(String ip, int port, AsynchronousChannelGroup channelGroup, ChannelContextBuilder serverChannelContextBuilder, AioListener serverListener)
	{
		this.ip = ip;
		this.port = port;
		this.channelGroup = channelGroup;
		acceptHandler = new AcceptHandler(serverSocketChannel, serverChannelContextBuilder, serverListener);
	}
	
	/**
	 * 以端口初始化server服务器。
	 * 
	 * @param port
	 */
	public void start()
	{
		try
		{
			serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
			serverSocketChannel.bind(new InetSocketAddress(ip, port), 0);
			serverSocketChannel.accept(null, acceptHandler);
		}
		catch (IOException e)
		{
			throw new JustThrowException(e);
		}
	}
	
	public void stop()
	{
		try
		{
			serverSocketChannel.close();
			channelGroup.shutdownNow();
			channelGroup.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
