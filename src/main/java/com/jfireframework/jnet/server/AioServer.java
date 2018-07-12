package com.jfireframework.jnet.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.TimeUnit;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.buffer.BufferAllocator;

public class AioServer
{
	private AsynchronousChannelGroup		channelGroup;
	private AsynchronousServerSocketChannel	serverSocketChannel;
	private String							ip;
	private int								port;
	private AcceptHandler					acceptHandler;
	
	public AioServer(String ip, int port, AsynchronousChannelGroup channelGroup, ChannelContextInitializer channelContextInitializer, AioListener serverListener, BufferAllocator allocator)
	{
		this.ip = ip;
		this.port = port;
		this.channelGroup = channelGroup;
		acceptHandler = new AcceptHandler(channelContextInitializer, serverListener, allocator);
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
			channelGroup.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
