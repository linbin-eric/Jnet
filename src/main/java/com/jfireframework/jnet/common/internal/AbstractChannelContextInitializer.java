package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;

public abstract class AbstractChannelContextInitializer implements ChannelContextInitializer
{
	private int batchWriteNum = 1;
	
	public AbstractChannelContextInitializer()
	{
		batchWriteNum = 1;
	}
	
	public AbstractChannelContextInitializer(int batchWriteNum)
	{
		this.batchWriteNum = batchWriteNum;
	}
	
	@Override
	public ReadCompletionHandler provideReadCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator)
	{
		return new DefaultReadCompletionHandler(aioListener, allocator, socketChannel);
	}
	
	@Override
	public WriteCompletionHandler provideWriteCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator)
	{
		if (batchWriteNum <= 1)
		{
			return new SingleWriteCompletionHandler(socketChannel, aioListener, allocator);
		}
		else
		{
			return new BatchWriteCompletionHandler(aioListener, socketChannel, allocator, batchWriteNum);
		}
	}
	
}
