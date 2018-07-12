package com.jfireframework.jnet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
	protected final ChannelContextInitializer	channelContextInitializer;
	protected final AioListener					aioListener;
	protected final BufferAllocator				allocator;
	
	public AcceptHandler(ChannelContextInitializer channelContextInitializer, AioListener aioListener, BufferAllocator allocator)
	{
		this.allocator = allocator;
		this.aioListener = aioListener;
		this.channelContextInitializer = channelContextInitializer;
	}
	
	@Override
	public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
	{
		WriteCompletionHandler writeCompletionHandle = channelContextInitializer.provideWriteCompletionHandler(socketChannel, aioListener, allocator);
		ChannelContext channelContext = new DefaultChannelContext(socketChannel, aioListener, writeCompletionHandle);
		writeCompletionHandle.bind(channelContext);
		channelContextInitializer.onChannelContextInit(channelContext);
		ReadCompletionHandler readCompletionHandler = channelContextInitializer.provideReadCompletionHandler(socketChannel, aioListener, allocator);
		readCompletionHandler.bind(channelContext);
		readCompletionHandler.start();
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
