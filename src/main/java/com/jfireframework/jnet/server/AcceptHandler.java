package com.jfireframework.jnet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextBuilder;
import com.jfireframework.jnet.common.api.Configuration;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
	protected final ChannelContextBuilder	channelContextBuilder;
	protected final AioListener				serverListener;
	
	public AcceptHandler(ChannelContextBuilder channelContextBuilder, AioListener serverListener)
	{
		this.channelContextBuilder = channelContextBuilder;
		this.serverListener = serverListener;
	}
	
	@Override
	public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
	{
		Configuration configuration = channelContextBuilder.onConnect(socketChannel, serverListener);
		ChannelContext serverChannelContext = configuration.config();
		channelContextBuilder.afterContextBuild(serverChannelContext);
		serverChannelContext.registerRead();
		serverChannel.accept(serverChannel, this);
	}
	
	@Override
	public void failed(Throwable exc, AsynchronousServerSocketChannel serverChannel)
	{
		try
		{
			serverChannel.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
