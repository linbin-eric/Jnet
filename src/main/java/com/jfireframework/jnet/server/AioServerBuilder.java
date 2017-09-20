package com.jfireframework.jnet.server;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.StringUtil;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelConnectListener;
import com.jfireframework.jnet.common.support.DefaultAioListener;

public class AioServerBuilder
{
	private AioListener					aioListener;
	private ChannelConnectListener		channelConnectListener;
	private int							port	= -1;
	private String						bindIp	= "0.0.0.0";
	private AsynchronousChannelGroup	channelGroup;
	
	public AioServer build()
	{
		try
		{
			if (channelConnectListener == null)
			{
				throw new NullPointerException(StringUtil.format("channelContextBuilder 不能为空"));
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
			AioServer aioServer = new AioServer(bindIp, port, channelGroup, channelConnectListener, aioListener);
			return aioServer;
		}
		catch (Throwable e)
		{
			throw new JustThrowException(e);
		}
	}
	
	public void setChannelConnectListener(ChannelConnectListener channelConnectListener)
	{
		this.channelConnectListener = channelConnectListener;
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
