package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class OutputProcessor implements DataProcessor<IoBuffer>
{
	private ChannelContext channelContext;
	
	@Override
	public void bind(ChannelContext channelContext)
	{
		this.channelContext = channelContext;
	}
	
	@Override
	public boolean process(IoBuffer data, ProcessorInvoker next) throws Throwable
	{
		if (channelContext.write(data))
		{
			return true;
		}
		else
		{
			channelContext
			return false;
		}
	}
	
}
