package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.processor.worker.FixedAttachWorker;

public class FixedAttachIoProcessor implements DataProcessor<Object>
{
	private final FixedAttachWorker	worker;
	private ChannelContext			channelContext;
	
	public FixedAttachIoProcessor(FixedAttachWorker worker)
	{
		this.worker = worker;
	}
	
	@Override
	public void bind(ChannelContext channelContext)
	{
		this.channelContext = channelContext;
	}
	
	@Override
	public void process(Object data, ProcessorInvoker next) throws Throwable
	{
		worker.commit(channelContext, next, data);
	}
	
}
