package com.jfireframework.jnet.common.configuration;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.support.DefaultChannelContext;

public abstract class AbstractConfiguration implements Configuration
{
	protected AioListener				aioListener;
	protected FrameDecodec				frameDecodec;
	protected StreamProcessor[]			inProcessors;
	protected StreamProcessor[]			outProcessors;
	protected int						maxMerge	= 10;
	protected AsynchronousSocketChannel	socketChannel;
	protected SendBufStorage			sendBufStorage;
	protected ByteBuf<?>				inCachedBuf;
	protected ByteBuf<?>				outCachedBuf;
	protected ReadProcessor				readProcessor;
	protected Object					attachment;
	
	public AbstractConfiguration(AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
	{
		this.aioListener = aioListener;
		this.frameDecodec = frameDecodec;
		this.inProcessors = inProcessors;
		this.outProcessors = outProcessors;
		this.maxMerge = maxMerge;
		this.socketChannel = socketChannel;
		this.sendBufStorage = sendBufStorage;
		this.inCachedBuf = inCachedBuf;
		this.outCachedBuf = outCachedBuf;
	}
	
	public AbstractConfiguration()
	{
	}
	
	@Override
	public ChannelContext config()
	{
		inProcessors = inProcessors == null ? new StreamProcessor[0] : inProcessors;
		outProcessors = outProcessors == null ? new StreamProcessor[0] : outProcessors;
		return new DefaultChannelContext(readProcessor, sendBufStorage, maxMerge, aioListener, inProcessors, outProcessors, socketChannel, frameDecodec, inCachedBuf, outCachedBuf, attachment);
	}
	
	public void setAttachment(Object attachment)
	{
		this.attachment = attachment;
	}
	
	public void setAioListener(AioListener aioListener)
	{
		this.aioListener = aioListener;
	}
	
	public void setFrameDecodec(FrameDecodec frameDecodec)
	{
		this.frameDecodec = frameDecodec;
	}
	
	public void setInProcessors(StreamProcessor[] inProcessors)
	{
		this.inProcessors = inProcessors;
	}
	
	public void setOutProcessors(StreamProcessor[] outProcessors)
	{
		this.outProcessors = outProcessors;
	}
	
	public void setMaxMerge(int maxMerge)
	{
		this.maxMerge = maxMerge;
	}
	
	public void setSocketChannel(AsynchronousSocketChannel socketChannel)
	{
		this.socketChannel = socketChannel;
	}
	
	public void setSendBufStorage(SendBufStorage sendBufStorage)
	{
		this.sendBufStorage = sendBufStorage;
	}
	
	public void setInCachedBuf(ByteBuf<?> inCachedBuf)
	{
		this.inCachedBuf = inCachedBuf;
	}
	
	public void setOutCachedBuf(ByteBuf<?> outCachedBuf)
	{
		this.outCachedBuf = outCachedBuf;
	}
	
	public void setReadProcessor(ReadProcessor readProcessor)
	{
		this.readProcessor = readProcessor;
	}
	
}
