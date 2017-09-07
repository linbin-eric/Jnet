package com.jfireframework.jnet.common.support;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadHandler;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.DecodeResult;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.exception.EndOfStreamException;
import com.jfireframework.jnet.common.exception.NotFitProtocolException;

public class DefaultReadHandler implements ReadHandler
{
	protected final FrameDecodec				frameDecodec;
	protected final ByteBuf<?>					ioBuf;
	protected final ChannelContext				channelContext;
	protected final AsynchronousSocketChannel	socketChannel;
	protected final AioListener					aioListener;
	protected final SendBufStorage				bufStorage;
	protected final StreamProcessor[]			inProcessors;
	protected volatile boolean					readPending	= false;
	
	public DefaultReadHandler(AioListener aioListener, ChannelContext channelContext)
	{
		this.aioListener = aioListener;
		this.channelContext = channelContext;
		socketChannel = channelContext.socketChannel();
		frameDecodec = channelContext.frameDecodec();
		ioBuf = channelContext.inCachedBuf();
		bufStorage = channelContext.sendBufStorage();
		inProcessors = channelContext.inProcessors();
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
			decodecAndProcess();
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
	
	public void decodecAndProcess() throws Throwable
	{
		while (true)
		{
			DecodeResult decodeResult = frameDecodec.decodec(ioBuf);
			switch (decodeResult.getType())
			{
				case LESS_THAN_PROTOCOL:
					return;
				case BUF_NOT_ENOUGH:
					ioBuf.compact().ensureCapacity(decodeResult.getNeed());
					return;
				case NOT_FIT_PROTOCOL:
					catchThrowable(NotFitProtocolException.instance, channelContext);
					return;
				case NORMAL:
					ByteBuf<?> packet = decodeResult.getBuf();
					channelContext.process(packet);
			}
		}
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
	
	public void registerRead()
	{
		if (readPending)
		{
			throw new UnsupportedOperationException();
		}
		readPending = true;
		aioListener.readRegister(channelContext);
		try
		{
			socketChannel.read(getWriteBuffer(), null, this);
		}
		catch (Exception e)
		{
			aioListener.catchException(e, channelContext);
		}
	}
	
}
