package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class SingleWriteCompletionHandler extends AbstractWriteCompletionHandler
{
	
	public SingleWriteCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator)
	{
		super(socketChannel, aioListener, allocator);
	}
	
	/**
	 * 从MPSCQueue中取得一个IoBuffer，并且执行写操作
	 */
	@Override
	protected void writeQueuedBuffer()
	{
		IoBuffer buffer = queue.poll();
		entry.setIoBuffer(buffer);
		entry.setByteBuffer(buffer.readableByteBuffer());
		socketChannel.write(entry.getByteBuffer(), entry, this);
	}
	
}
