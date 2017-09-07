package com.jfireframework.jnet.common.configuration;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.StreamProcessor;
import com.jfireframework.jnet.common.bufstorage.impl.MpscBufStorage;
import com.jfireframework.jnet.common.bufstorage.impl.SpscBufStorage;
import com.jfireframework.jnet.common.configuration.MutliAttachConfiguration.MutlisAttachProcessor;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.support.DefaultAioListener;

public class ConfigurationTemplate
{
	public static Configuration simple(FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, AsynchronousSocketChannel socketChannel)
	{
		return new SimpleConfiguration(new DefaultAioListener(), frameDecodec, inProcessors, outProcessors, 10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
	}
	
	public static Configuration channelAttch(ExecutorService executorService, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, AsynchronousSocketChannel socketChannel)
	{
		return new ChannelAttachConfiguration(executorService, new DefaultAioListener(), frameDecodec, inProcessors, outProcessors, 10, socketChannel, new SpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
	}
	
	public static Configuration threadAttch(ExecutorService executorService, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, AsynchronousSocketChannel socketChannel)
	{
		return new ThreadAttchConfiguration(executorService, new DefaultAioListener(), frameDecodec, inProcessors, outProcessors, 10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
	}
	
	public static Configuration mutliAttch(MutlisAttachProcessor processor, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, AsynchronousSocketChannel socketChannel)
	{
		return new MutliAttachConfiguration(processor, new DefaultAioListener(), frameDecodec, inProcessors, outProcessors, 10, socketChannel, new MpscBufStorage(), DirectByteBuf.allocate(128), DirectByteBuf.allocate(128));
	}
}
