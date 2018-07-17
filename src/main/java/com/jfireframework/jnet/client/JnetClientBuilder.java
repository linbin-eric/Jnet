package com.jfireframework.jnet.client;

import java.nio.channels.AsynchronousChannelGroup;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.internal.DefaultAioListener;

public class JnetClientBuilder
{
	
	// 服务器的启动端口
	private int							port		= -1;
	/**
	 * 处理socket事件的起始线程数。如果线程池模式选择固定线程数模式的话，则这个数值就是线程数的值。如果线程池模式选择cache模式的话，则这个数值是初始线程数。
	 */
	private String						serverIp	= "0.0.0.0";
	private AsynchronousChannelGroup	channelGroup;
	private AioListener					aioListener;
	private ChannelContextInitializer	channelContextInitializer;
	private BufferAllocator				allocator;
	
	public JnetClient build()
	{
		try
		{
			if (channelContextInitializer == null)
			{
				throw new NullPointerException();
			}
			if (aioListener == null)
			{
				aioListener = new DefaultAioListener();
			}
			if (allocator == null)
			{
				throw new NullPointerException();
			}
			return new DefaultClient(channelContextInitializer, serverIp, port, aioListener, allocator, channelGroup);
		}
		catch (Throwable e)
		{
			ReflectUtil.throwException(e);
			return null;
		}
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public void setServerIp(String serverIp)
	{
		this.serverIp = serverIp;
	}
	
	public void setChannelGroup(AsynchronousChannelGroup channelGroup)
	{
		this.channelGroup = channelGroup;
	}
	
	public void setAioListener(AioListener aioListener)
	{
		this.aioListener = aioListener;
	}
	
	public void setChannelContextInitializer(ChannelContextInitializer channelContextInitializer)
	{
		this.channelContextInitializer = channelContextInitializer;
	}
	
	public void setAllocator(BufferAllocator allocator)
	{
		this.allocator = allocator;
	}
	
}
