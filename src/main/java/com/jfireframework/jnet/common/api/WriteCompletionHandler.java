package com.jfireframework.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface WriteCompletionHandler extends CompletionHandler<Integer, WriteEntry>
{
	/**
	 * 绑定与该写完成器使用通道关联的ChannelContext
	 * 
	 * @param channelContext
	 */
	void bind(ChannelContext channelContext);
	
	/**
	 * 提供数据供写出<br/>
	 * 如果当前写完成器已经处于停止状态，则抛出非法状态异常
	 * 
	 * @param buffer
	 * @throws IllegalStateException
	 */
	void offer(IoBuffer buffer) throws IllegalStateException;
	
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
	
}
