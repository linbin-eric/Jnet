package com.jfireframework.jnet.common.channelcontext;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class SimpleConfiguration implements Configuration
{
	protected AioListener				aioListener;
	protected FrameDecodec				frameDecodec;
	protected StreamProcessor[]			inProcessors;
	protected StreamProcessor[]			outProcessors;
	private int							maxMerge	= 10;
	protected AsynchronousSocketChannel	socketChannel;
	private SendBufStorage				sendBufStorage;
	private ByteBuf<?>					inCachedBuf;
	private ByteBuf<?>					outCachedBuf;
	private ReadProcessor				readProcessor;
	
	public SimpleConfiguration(AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
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
		readProcessor = new ReadProcessor() {
			
			@Override
			public void process(ByteBuf<?> buf, SendBufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
			{
				Object finalResult = ProcesserUtil.process(channelContext, inProcessors, buf, 0);
				if (finalResult instanceof ByteBuf<?>)
				{
					bufStorage.putBuf(buf);
					channelContext.registerWrite();
				}
			}
		};
	}
	
	@Override
	public ChannelContext config()
	{
		return new BaseChannelContext(readProcessor, sendBufStorage, maxMerge, aioListener, inProcessors, outProcessors, socketChannel, frameDecodec, inCachedBuf, outCachedBuf);
	}
	
}
