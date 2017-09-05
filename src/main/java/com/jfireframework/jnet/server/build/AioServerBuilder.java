package com.jfireframework.jnet.server.build;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.util.DefaultAioListener;
import com.jfireframework.jnet.server.AioServer;

public class AioServerBuilder
{
	private AioListener					aioListener;
	private ChannelContextBuilder		channelContextBuilder;
	private int							port	= -1;
	private String						bindIp	= "0.0.0.0";
	private AsynchronousChannelGroup	channelGroup;
	
	public AioServer build()
	{
		try
		{
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
			AioServer aioServer = new AioServer(bindIp, port, channelGroup, channelContextBuilder, aioListener);
			return aioServer;
		}
		catch (Throwable e)
		{
			throw new JustThrowException(e);
		}
	}
	
	public void setChannelContextBuilder(ChannelContextBuilder channelContextBuilder)
	{
		this.channelContextBuilder = channelContextBuilder;
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
	
}
