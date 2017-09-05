package com.jfireframework.jnet.common.build;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.Configuration;

public interface ChannelContextBuilder
{
	/**
	 * 当客户端连接建立的时候触发
	 */
	public Configuration onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener);
	
	public void afterContextBuild(ChannelContext serverChannelContext);
}
