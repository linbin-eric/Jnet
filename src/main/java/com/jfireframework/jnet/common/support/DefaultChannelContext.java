package com.jfireframework.jnet.common.support;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.util.ChainUtil;
import com.jfireframework.pool.ioBuffer.IoBuffer;

public class DefaultChannelContext implements ChannelContext
{
	private volatile Object					attachment;
	private final WriteHandler				writeHandler;
	private final AsynchronousSocketChannel	socketChannel;
	private final ProcessorChain			chain;
	
	public DefaultChannelContext(AsynchronousSocketChannel socketChannel, int maxMerge, AioListener aioListener, ReadProcessor<?>... readProcessors)
	{
		this.socketChannel = socketChannel;
		writeHandler = new WriteHandler(aioListener, this, maxMerge);
		for (ReadProcessor<?> each : readProcessors)
		{
			each.initialize(this);
		}
		chain = ChainUtil.parse(readProcessors, this);
	}
	
	@Override
	public Object getAttachment()
	{
		return attachment;
	}
	
	@Override
	public void setAttachment(Object attachment)
	{
		this.attachment = attachment;
	}
	
	@Override
	public void read(IoBuffer packet) throws Throwable
	{
		chain.chain(packet);
	}
	
	@Override
	public void write(IoBuffer buf)
	{
		writeHandler.write(buf);
	}
	
	@Override
	public AsynchronousSocketChannel socketChannel()
	{
		return socketChannel;
	}
	
}
