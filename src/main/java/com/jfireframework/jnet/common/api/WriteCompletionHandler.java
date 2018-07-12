package com.jfireframework.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface WriteCompletionHandler extends CompletionHandler<Integer, WriteEntry>
{
	class WriteEntry
	{
		ByteBuffer	byteBuffer;
		IoBuffer	ioBuffer;
		
		public ByteBuffer getByteBuffer()
		{
			return byteBuffer;
		}
		
		public void setByteBuffer(ByteBuffer byteBuffer)
		{
			this.byteBuffer = byteBuffer;
		}
		
		public IoBuffer getIoBuffer()
		{
			return ioBuffer;
		}
		
		public void setIoBuffer(IoBuffer ioBuffer)
		{
			this.ioBuffer = ioBuffer;
		}
		
		public void clear()
		{
			ioBuffer = null;
			byteBuffer = null;
		}
	}
	
	/**
	 * 将数据写出。如果当前写完成器已经停止工作，则会抛出IllegalStateException。此时主方法应该将buffer释放
	 * 
	 * @param buffer
	 * @throws IllegalStateException
	 */
	void write(IoBuffer buffer) throws IllegalStateException;
	
	/**
	 * 绑定与该写完成器使用通道关联的ChannelContext
	 * 
	 * @param channelContext
	 */
	void bind(ChannelContext channelContext);
}
