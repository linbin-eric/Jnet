package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.jnet.common.api.ChannelContext;

public class ProcessorTask
{
	private Object			data;
	private ChannelContext	channelContext;
	private int				initIndex;
	
	public ProcessorTask(Object data, int initIndex, ChannelContext channelContext)
	{
		this.data = data;
		this.initIndex = initIndex;
		this.channelContext = channelContext;
	}
	
	public int getInitIndex()
	{
		return initIndex;
	}
	
	public void setInitIndex(int initIndex)
	{
		this.initIndex = initIndex;
	}
	
	public Object getData()
	{
		return data;
	}
	
	public void setData(Object data)
	{
		this.data = data;
	}
	
	public ChannelContext getChannelContext()
	{
		return channelContext;
	}
	
	public void setChannelContext(ChannelContext channelContext)
	{
		this.channelContext = channelContext;
	}
	
}
