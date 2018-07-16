package com.jfireframework.jnet.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AcceptHandler;

public class AioServer
{
	private AsynchronousChannelGroup		channelGroup;
	private AsynchronousServerSocketChannel	serverSocketChannel;
	private String							ip;
	private int								port;
	private AcceptHandler					acceptHandler;
	
	public AioServer(String ip, int port, AsynchronousChannelGroup channelGroup, AcceptHandler acceptHandler)
	{
		this.ip = ip;
		this.port = port;
		this.channelGroup = channelGroup;
		this.acceptHandler = acceptHandler;
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
			serverSocketChannel.accept(serverSocketChannel, acceptHandler);
		}
		catch (IOException e)
		{
			ReflectUtil.throwException(e);
		}
	}
	
	public void stop()
	{
		try
		{
			serverSocketChannel.close();
			channelGroup.shutdownNow();
		}
		catch (Exception e)
		{
			ReflectUtil.throwException(e);
		}
	}
}
