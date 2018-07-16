package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultReadCompletionHandler implements ReadCompletionHandler
{
	protected final AsynchronousSocketChannel	socketChannel;
	protected final AioListener					aioListener;
	protected final BufferAllocator				allocator;
	protected final ReadEntry					entry	= new ReadEntry();
	protected ChannelContext					channelContext;
	
	public DefaultReadCompletionHandler(AioListener aioListener, BufferAllocator allocator, AsynchronousSocketChannel socketChannel)
	{
		this.aioListener = aioListener;
		this.allocator = allocator;
		this.socketChannel = socketChannel;
	}
	
	@Override
	public void start()
	{
		IoBuffer buffer = allocator.ioBuffer(128);
		entry.setIoBuffer(buffer);
		entry.setByteBuffer(buffer.writableByteBuffer());
		socketChannel.read(entry.getByteBuffer(), entry, this);
	}
	
	@Override
	public void completed(Integer read, ReadEntry entry)
	{
		if (read == -1)
		{
			try
			{
				socketChannel.close();
			}
			catch (Throwable e)
			{
				ReflectUtil.throwException(e);
			}
			finally
			{
				entry.getIoBuffer().free();
			}
			return;
		}
		IoBuffer buffer = entry.getIoBuffer();
		buffer.addWritePosi(read);
		try
		{
			channelContext.process(buffer);
		}
		catch (Throwable e)
		{
			buffer.free();
			try
			{
				socketChannel.close();
			}
			catch (IOException e1)
			{
				;
			}
			return;
		}
		if (buffer.getReadPosi() > 4096)
		{
			buffer.compact();
		}
		entry.setIoBuffer(buffer);
		entry.setByteBuffer(buffer.writableByteBuffer());
		socketChannel.read(entry.getByteBuffer(), entry, this);
	}
	
	@Override
	public void failed(Throwable exc, ReadEntry entry)
	{
		entry.getIoBuffer().free();
		if (aioListener != null)
		{
			aioListener.catchException(exc, socketChannel);
		}
	}
	
	@Override
	public void bind(ChannelContext channelContext)
	{
		this.channelContext = channelContext;
	}
	
}
