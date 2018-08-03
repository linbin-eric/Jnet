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
				catchException(e);
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
			if (channelContext.process(buffer) == false)
			{
				return;
			}
		}
		catch (Throwable e)
		{
			catchException(e);
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
		if (needCompact(buffer))
		{
			buffer.compact();
		}
		entry.setIoBuffer(buffer);
		entry.setByteBuffer(buffer.writableByteBuffer());
		socketChannel.read(entry.getByteBuffer(), entry, this);
	}
	
	private boolean needCompact(IoBuffer buffer)
	{
		return buffer.getReadPosi() > 1024 * 1024 && buffer.remainRead() < 1024;
	}
	
	private void catchException(Throwable e)
	{
		if (aioListener != null)
		{
			aioListener.catchException(e, socketChannel);
		}
	}
	
	@Override
	public void failed(Throwable exc, ReadEntry entry)
	{
		entry.getIoBuffer().free();
		catchException(exc);
	}
	
	@Override
	public void bind(ChannelContext channelContext)
	{
		this.channelContext = channelContext;
	}
	
	@Override
	public void continueRead()
	{
		IoBuffer buffer = entry.getIoBuffer();
		try
		{
			boolean process = channelContext.process(buffer);
			if (process == false)
			{
				return;
			}
		}
		catch (Throwable e)
		{
			catchException(e);
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
		if (needCompact(buffer))
		{
			buffer.compact();
		}
		entry.setByteBuffer(buffer.writableByteBuffer());
		socketChannel.read(entry.getByteBuffer(), entry, this);
	}
	
}
