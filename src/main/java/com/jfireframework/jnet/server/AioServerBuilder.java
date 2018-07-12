package com.jfireframework.jnet.server;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.StringUtil;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.internal.DefaultAioListener;

public class AioServerBuilder
{
	private AioListener					aioListener;
	private ChannelContextInitializer	channelContextInitializer;
	private int							port	= -1;
	private String						bindIp	= "0.0.0.0";
	private AsynchronousChannelGroup	channelGroup;
	private BufferAllocator				allocator;
	
	public AioServer build()
	{
		try
		{
			if (channelContextInitializer == null)
			{
				throw new NullPointerException(StringUtil.format("channelContextInitializer 不能为空"));
			}
			if (channelGroup == null)
			{
				channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new ThreadFactory() {
					int i = 1;
					
					@Override
					public Thread newThread(Runnable r)
					{
						return new Thread(r, "服务端IO线程-" + (i++));
					}
				});
			}
			if (aioListener == null)
			{
				aioListener = new DefaultAioListener();
			}
			AioServer aioServer = new AioServer(bindIp, port, channelGroup, channelContextInitializer, aioListener, allocator);
			return aioServer;
		}
		catch (Throwable e)
		{
			ReflectUtil.throwException(e);
			return null;
		}
	}
	
	public AioServerBuilder setChannelContextInitializer(ChannelContextInitializer channelContextInitializer)
	{
		this.channelContextInitializer = channelContextInitializer;
		return this;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public void setBindIp(String bindIp)
	{
		this.bindIp = bindIp;
	}
	
	public void setChannelGroup(AsynchronousChannelGroup channelGroup)
	{
		this.channelGroup = channelGroup;
	}
	
	public void setAioListener(AioListener aioListener)
	{
		this.aioListener = aioListener;
	}
	
	public AioServerBuilder setAllocator(BufferAllocator allocator)
	{
		this.allocator = allocator;
		return this;
	}
}
