package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.StreamProcessor;

public class ProcesserUtil
{
	public static Object process(ChannelContext context, StreamProcessor[] processors, Object target) throws Throwable
	{
		for (StreamProcessor each : processors)
		{
			target = each.process(target, context);
		}
		return target;
	}
	
}
