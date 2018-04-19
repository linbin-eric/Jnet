package com.jfireframework.jnet.common.processor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;

public class CommonPoolProcessor implements ReadProcessor<Object>
{
	private ExecutorService executorService;
	
	public CommonPoolProcessor(ExecutorService executorService)
	{
		this.executorService = executorService;
	}

	@Override
	public void initialize(ChannelContext channelContext)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void process(final Object data, final ProcessorChain chain, final ChannelContext channelContext) throws Throwable
	{
		executorService.submit(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					chain.chain(data);
				}
				catch (Throwable e)
				{
					try
					{
						channelContext.socketChannel().close();
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		});
	}
	
}
