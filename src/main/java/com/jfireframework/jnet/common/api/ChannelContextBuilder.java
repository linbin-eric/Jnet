package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelContextBuilder
{
	/**
	 * 当客户端连接建立的时候触发
	 */
	public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener);
	
	public void afterContextBuild(ChannelContext serverChannelContext);
}
