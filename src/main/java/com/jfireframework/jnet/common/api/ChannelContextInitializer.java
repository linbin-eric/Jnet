package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.buffer.BufferAllocator;

public interface ChannelContextInitializer
{
	
	ReadCompletionHandler provideReadCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator);
	
	WriteCompletionHandler provideWriteCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator);
	
	void onChannelContextInit(ChannelContext channelContext);
	
}
