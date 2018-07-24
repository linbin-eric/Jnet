package com.jfireframework.jnet.common.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface BackPressureService
{
	void submit(ChannelContext channelContext, Object data, DataProcessor<Object> current, ReadCompletionHandler readCompletionHandler);
	
	BackPressureService DEFAULT = new BackPressureService() {
		ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		
		@Override
		public void submit(final ChannelContext channelContext, final Object data, final DataProcessor<Object> current, final ReadCompletionHandler readCompletionHandler)
		{
			EXECUTOR_SERVICE.submit(new Runnable() {
				
				@Override
				public void run()
				{
					try
					{
						while (current.process(data) == false)
						{
							Thread.yield();
						}
						readCompletionHandler.continueRead();
					}
					catch (Throwable e)
					{
						channelContext.close(e);
					}
				}
			});
		}
	};
}
