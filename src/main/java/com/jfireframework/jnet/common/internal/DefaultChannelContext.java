package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.BackPressureService;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultChannelContext implements ChannelContext
{
	private WriteCompletionHandler		writeCompletionHandler;
	private ReadCompletionHandler		readCompletionHandler;
	private ProcessorInvoker			invoker;
	private AsynchronousSocketChannel	socketChannel;
	private BackPressureService			backPressureService;
	private AioListener					aioListener;
	
	public DefaultChannelContext(AsynchronousSocketChannel socketChannel, AioListener aioListener, BackPressureService backPressureService)
	{
		this.socketChannel = socketChannel;
		this.aioListener = aioListener;
		this.backPressureService = backPressureService;
	}
	
	@Override
	public boolean write(IoBuffer buffer)
	{
		return writeCompletionHandler.offer(buffer);
	}
	
	@Override
	public AsynchronousSocketChannel socketChannel()
	{
		return socketChannel;
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setDataProcessor(DataProcessor<?>... dataProcessors)
	{
		ProcessorInvoker last = new ProcessorInvoker() {
			
			@Override
			public boolean process(Object data) throws Throwable
			{
				throw new NullPointerException();
			}
			
		};
		ProcessorInvoker prev = last;
		for (int i = dataProcessors.length - 1; i >= 0; i--)
		{
			final ProcessorInvoker next = prev;
			final DataProcessor processor = dataProcessors[i];
			ProcessorInvoker invoker = new ProcessorInvoker() {
				
				@Override
				public boolean process(Object data) throws Throwable
				{
					return processor.process(data, next);
				}
				
			};
			prev = invoker;
		}
		invoker = prev;
		for (DataProcessor<?> each : dataProcessors)
		{
			each.bind(this);
		}
	}
	
	@Override
	public boolean process(IoBuffer buffer) throws Throwable
	{
		return invoker.process(buffer);
	}
	
	@Override
	public void bind(WriteCompletionHandler writeCompletionHandler, ReadCompletionHandler readCompletionHandler)
	{
		this.writeCompletionHandler = writeCompletionHandler;
		this.readCompletionHandler = readCompletionHandler;
	}
	
	@Override
	public void close()
	{
		try
		{
			socketChannel.close();
			aioListener.onClose(socketChannel, null);
		}
		catch (IOException e)
		{
			;
		}
	}
	
	@Override
	public void close(Throwable e)
	{
		try
		{
			socketChannel.close();
			aioListener.onClose(socketChannel, e);
		}
		catch (IOException e1)
		{
			;
		}
	}
	
	@Override
	public void submitBackPressureTask(ProcessorInvoker next, Object data)
	{
		backPressureService.submit(this, data, next, readCompletionHandler);
	}
	
}
