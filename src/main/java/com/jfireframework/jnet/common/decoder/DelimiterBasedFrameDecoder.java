package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;

/**
 * 特定结束符整包解码器
 * 
 * @author 林斌
 * 
 */
public class DelimiterBasedFrameDecoder implements DataProcessor<IoBuffer>
{
	private byte[]			delimiter;
	private int				maxLength;
	private BufferAllocator	allocator;
	
	/**
	 * 
	 * @param delimiter 解码使用的特定字节数组
	 * @param maxLength 读取的码流最大长度。超过这个长度还未发现结尾分割字节数组，就会抛出异常
	 */
	public DelimiterBasedFrameDecoder(byte[] delimiter, int maxLength, BufferAllocator allocator)
	{
		this.maxLength = maxLength;
		this.delimiter = delimiter;
		this.allocator = allocator;
	}
	
	@Override
	public void initialize(ChannelContext channelContext)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void process(IoBuffer ioBuf, ProcessorChain chain, ChannelContext channelContext) throws Throwable
	{
		do
		{
			if (ioBuf.remainRead() > maxLength)
			{
				throw new TooLongException();
			}
			int index = ioBuf.indexOf(delimiter);
			if (index == -1)
			{
				ioBuf.compact().capacityReadyFor(ioBuf.capacity() * 2);
				return;
			}
			else
			{
				int contentLength = index - ioBuf.getReadPosi();
				IoBuffer buf = allocator.ioBuffer(contentLength);
				buf.put(ioBuf, contentLength);
				ioBuf.setReadPosi(index + delimiter.length);
				chain.chain(buf);
			}
		} while (true);
	}
	
}
