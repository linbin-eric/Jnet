package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class FixLengthDecoder implements DataProcessor<IoBuffer>
{
	private final int		frameLength;
	private BufferAllocator	allocator;
	
	/**
	 * 固定长度解码器
	 * 
	 * @param frameLength 一个报文的固定长度
	 */
	public FixLengthDecoder(int frameLength, BufferAllocator allocator)
	{
		this.frameLength = frameLength;
		this.allocator = allocator;
	}
	
	@Override
	public void bind(ChannelContext channelContext)
	{
		
	}
	
	@Override
	public boolean process(IoBuffer ioBuf, ProcessorInvoker next) throws Throwable
	{
		do
		{
			if (ioBuf.remainRead() < frameLength)
			{
				ioBuf.compact().capacityReadyFor(frameLength);
				return true;
			}
			IoBuffer packet = allocator.ioBuffer(frameLength);
			packet.put(ioBuf, frameLength);
			ioBuf.addReadPosi(frameLength);
			if (next.process(packet) == false)
			{
				return false;
			}
		} while (true);
	}
	
	
}
