package com.jfireframework.jnet.common.configuration;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;

public class SimpleConfiguration extends AbstractConfiguration
{
	
	public SimpleConfiguration(AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
	{
		super(aioListener, frameDecodec, inProcessors, outProcessors, maxMerge, socketChannel, sendBufStorage, inCachedBuf, outCachedBuf);
		readProcessor = new ReadProcessor() {
			
			@Override
			public void process(ByteBuf<?> buf, ChannelContext channelContext) throws Throwable
			{
				StreamProcessor[] inProcessors = channelContext.inProcessors();
				Object finalResult = ProcesserUtil.process(channelContext, inProcessors, buf);
				if (finalResult instanceof ByteBuf<?>)
				{
					SendBufStorage bufStorage = channelContext.sendBufStorage();
					bufStorage.putBuf(buf);
					channelContext.registerWrite();
				}
			}
		};
	}
	
}
