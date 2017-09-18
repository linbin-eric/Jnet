package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelConnectListener
{
	/**
	 * 当链接建立时触发
	 */
	Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener);
	
}
