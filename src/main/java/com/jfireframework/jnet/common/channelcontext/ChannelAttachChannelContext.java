package com.jfireframework.jnet.common.channelcontext;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.readhandler.DefaultReadHandler;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ChannelAttachChannelContext extends BaseChannelContext
{
	
	public ChannelAttachChannelContext(SendBufStorage sendBufStorage, int maxMerge, AioListener aioListener, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, AsynchronousSocketChannel socketChannel, FrameDecodec frameDecodec, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
	{
		super(sendBufStorage, maxMerge, aioListener, inProcessors, outProcessors, socketChannel, frameDecodec, inCachedBuf, outCachedBuf);
		ReadProcessor readProcessor = new ReadProcessor() {
			
			@Override
			public void process(ByteBuf<?> buf, SendBufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
			{
				ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
				processor.commit(task);
			}
		};
		readHandler = new DefaultReadHandler(readProcessor, socketChannel, frameDecodec, outCachedBuf, aioListener, this);
	}
	
}
