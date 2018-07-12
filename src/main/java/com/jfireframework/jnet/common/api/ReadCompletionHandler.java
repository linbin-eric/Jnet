package com.jfireframework.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.ReadCompletionHandler.ReadEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface ReadCompletionHandler extends CompletionHandler<Integer, ReadEntry>
{
	class ReadEntry
	{
		IoBuffer	ioBuffer;
		ByteBuffer	byteBuffer;
		
		public IoBuffer getIoBuffer()
		{
			return ioBuffer;
		}
		
		public void setIoBuffer(IoBuffer ioBuffer)
		{
			this.ioBuffer = ioBuffer;
		}
		
		public ByteBuffer getByteBuffer()
		{
			return byteBuffer;
		}
		
		public void setByteBuffer(ByteBuffer byteBuffer)
		{
			this.byteBuffer = byteBuffer;
		}
		
		public void clear()
		{
			ioBuffer = null;
			byteBuffer = null;
		}
	}
	
	/**
	 * 开始监听数据
	 */
	void start();
	
	void bind(ChannelContext channelContext);
}
