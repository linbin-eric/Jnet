package com.jfireframework.jnet.common.channelcontext;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.resource.ResourceCloseAgent;
import com.jfireframework.baseutil.resource.ResourceCloseCallback;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadHandler;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.readhandler.DefaultReadHandler;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;
import com.jfireframework.jnet.common.writehandler.DefaultWriteHandler;

public class BaseChannelContext implements ChannelContext
{
	protected ReadHandler								readHandler;
	protected final WriteHandler						writeHandler;
	protected final AioListener							aioListener;
	protected final FrameDecodec						frameDecodec;
	protected final SendBufStorage						sendBufStorage;
	protected final ByteBuf<?>							inCachedBuf;
	protected final ByteBuf<?>							outCachedBuf;
	protected final StreamProcessor[]					inProcessors;
	protected final StreamProcessor[]					outProcessors;
	protected final AsynchronousSocketChannel			socketChannel;
	protected final ResourceCloseAgent<ChannelContext>	closeAgent	= new ResourceCloseAgent<ChannelContext>(this, new ResourceCloseCallback<ChannelContext>() {
																		
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
	
	public BaseChannelContext(//
	        ReadProcessor readProcessor, //
	        SendBufStorage sendBufStorage, //
	        int maxMerge, //
	        AioListener aioListener, //
	        StreamProcessor[] inProcessors, //
	        StreamProcessor[] outProcessors, //
	        AsynchronousSocketChannel socketChannel, //
	        FrameDecodec frameDecodec, //
	        ByteBuf<?> inCachedBuf, //
	        ByteBuf<?> outCachedBuf)
	{
		this.socketChannel = socketChannel;
		this.inProcessors = inProcessors;
		this.outProcessors = outProcessors;
		this.sendBufStorage = sendBufStorage;
		this.aioListener = aioListener;
		this.frameDecodec = frameDecodec;
		this.inCachedBuf = inCachedBuf;
		this.outCachedBuf = outCachedBuf;
		writeHandler = new DefaultWriteHandler(outCachedBuf, maxMerge, socketChannel, aioListener, sendBufStorage, this);
		readHandler = new DefaultReadHandler(readProcessor, socketChannel, frameDecodec, inCachedBuf, aioListener, this);
	}
	
	@Override
	public void push(Object send, int index) throws Throwable
	{
		Object finalResult = ProcesserUtil.process(this, outProcessors, send, index);
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
	
}
