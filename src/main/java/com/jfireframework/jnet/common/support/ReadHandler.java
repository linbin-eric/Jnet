package com.jfireframework.jnet.common.support;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.exception.EndOfStreamException;

public class ReadHandler implements CompletionHandler<Integer, Void>
{
	protected final ByteBuf<?>					ioBuf	= DirectByteBuf.allocate(1024);
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
		if (read == -1)
		{
			catchThrowable(EndOfStreamException.instance, channelContext);
			return;
		}
		ioBuf.addWriteIndex(read);
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
		ByteBuffer ioBuffer = ioBuf.nioBuffer();
		ioBuffer.position(ioBuffer.limit()).limit(ioBuffer.capacity());
		return ioBuffer;
	}
	
	protected void catchThrowable(Throwable e, ChannelContext context)
	{
		aioListener.catchException(e, context);
	}
	
}
