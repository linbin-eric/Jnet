package com.jfireframework.jnet.common.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.util.Allocator;
import com.jfireframework.pool.ioBuffer.IoBuffer;

public class ReadHandler implements CompletionHandler<Integer, Void>
{
	protected final IoBuffer					ioBuf	= Allocator.allocateDirect(1024);
	protected final ChannelContext				channelContext;
	protected final AsynchronousSocketChannel	socketChannel;
	protected final AioListener					aioListener;
	
	public ReadHandler(AioListener aioListener, ChannelContext channelContext)
	{
		this.aioListener = aioListener;
		this.channelContext = channelContext;
		socketChannel = channelContext.socketChannel();
	}
	
	public void start()
	{
		socketChannel.read(getWriteBuffer(), null, this);
	}
	
	@Override
	public void completed(Integer read, Void nothing)
	{
		// read为-1意味着输入流到了终点，但是还有输出流，所以不可以关闭通道
		if (read == -1)
		{
			try
			{
				socketChannel.close();
			}
			catch (IOException e)
			{
				catchThrowable(e, channelContext);
			}
			return;
		}
		ioBuf.addWritePosi(read);
		try
		{
			channelContext.read(ioBuf);
			socketChannel.read(getWriteBuffer(), null, this);
		}
		catch (Throwable e)
		{
			catchThrowable(e, channelContext);
		}
	}
	
	@Override
	public void failed(Throwable exc, Void nothing)
	{
		catchThrowable(exc, channelContext);
	}
	
	/**
	 * 将iobuf的内容进行压缩，返回一个处于可写状态的ByteBuffer
	 * 
	 * @return
	 */
	protected ByteBuffer getWriteBuffer()
	{
		ByteBuffer ioBuffer = ioBuf.byteBuffer();
		ioBuffer.position(ioBuffer.limit()).limit(ioBuffer.capacity());
		return ioBuffer;
	}
	
	protected void catchThrowable(Throwable e, ChannelContext context)
	{
		aioListener.catchException(e, context);
	}
	
}
