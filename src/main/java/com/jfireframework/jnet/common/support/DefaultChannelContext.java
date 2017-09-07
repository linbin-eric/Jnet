package com.jfireframework.jnet.common.support;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.resource.ResourceCloseAgent;
import com.jfireframework.baseutil.resource.ResourceCloseCallback;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadHandler;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;

public class DefaultChannelContext implements ChannelContext
{
	private final ReadHandler							readHandler;
	private final WriteHandler							writeHandler;
	private final SendBufStorage						sendBufStorage;
	private final ByteBuf<?>							inCachedBuf;
	private final ByteBuf<?>							outCachedBuf;
	private final StreamProcessor[]						inProcessors;
	private final StreamProcessor[]						outProcessors;
	private final AsynchronousSocketChannel				socketChannel;
	private final ResourceCloseAgent<ChannelContext>	closeAgent	= new ResourceCloseAgent<ChannelContext>(this, new ResourceCloseCallback<ChannelContext>() {
																		
																		@Override
																		public void onClose(ChannelContext resource)
																		{
																			try
																			{
																				socketChannel.close();
																			}
																			catch (IOException e)
																			{
																				e.printStackTrace();
																			}
																			finally
																			{
																				inCachedBuf.release();
																				outCachedBuf.release();
																			}
																		}
																	});
	private final Object								attachment;
	private final ReadProcessor							readProcessor;
	private final FrameDecodec							frameDecodec;
	private final int									maxMerge;
	
	public DefaultChannelContext(//
	        ReadProcessor readProcessor, //
	        SendBufStorage sendBufStorage, //
	        int maxMerge, //
	        AioListener aioListener, //
	        StreamProcessor[] inProcessors, //
	        StreamProcessor[] outProcessors, //
	        AsynchronousSocketChannel socketChannel, //
	        FrameDecodec frameDecodec, //
	        ByteBuf<?> inCachedBuf, //
	        ByteBuf<?> outCachedBuf, //
	        Object attachment)
	{
		this.readProcessor = readProcessor;
		this.socketChannel = socketChannel;
		this.inProcessors = inProcessors;
		this.outProcessors = outProcessors;
		this.sendBufStorage = sendBufStorage;
		this.inCachedBuf = inCachedBuf;
		this.outCachedBuf = outCachedBuf;
		this.frameDecodec = frameDecodec;
		this.maxMerge = maxMerge;
		this.attachment = attachment;
		writeHandler = new DefaultWriteHandler(aioListener, this);
		readHandler = new DefaultReadHandler(aioListener, this);
	}
	
	@Override
	public void push(Object send, int index) throws Throwable
	{
		Object finalResult = ProcesserUtil.process(this, outProcessors, send);
		if (finalResult instanceof ByteBuf<?>)
		{
			sendBufStorage.putBuf((ByteBuf<?>) finalResult);
			writeHandler.registerWrite();
		}
	}
	
	@Override
	public void registerWrite()
	{
		writeHandler.registerWrite();
	}
	
	@Override
	public boolean close()
	{
		return closeAgent.close();
	}
	
	@Override
	public boolean isOpen()
	{
		return closeAgent.isOpen();
	}
	
	@Override
	public StreamProcessor[] inProcessors()
	{
		return inProcessors;
	}
	
	@Override
	public StreamProcessor[] outProcessors()
	{
		return outProcessors;
	}
	
	@Override
	public SendBufStorage sendBufStorage()
	{
		return sendBufStorage;
	}
	
	@Override
	public AsynchronousSocketChannel socketChannel()
	{
		return socketChannel;
	}
	
	@Override
	public void registerRead()
	{
		readHandler.registerRead();
	}
	
	@Override
	public Object attachment()
	{
		return attachment;
	}
	
	@Override
	public void process(ByteBuf<?> packet) throws Throwable
	{
		readProcessor.process(packet, this);
	}
	
	@Override
	public ByteBuf<?> inCachedBuf()
	{
		return inCachedBuf;
	}
	
	@Override
	public ByteBuf<?> outCachedBuf()
	{
		return outCachedBuf;
	}
	
	@Override
	public FrameDecodec frameDecodec()
	{
		return frameDecodec;
	}
	
	@Override
	public int maxMerge()
	{
		return maxMerge;
	}
	
}
