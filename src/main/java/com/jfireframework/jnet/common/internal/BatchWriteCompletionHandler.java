package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class BatchWriteCompletionHandler extends AbstractWriteCompletionHandler
{
	private final int maxMerge;
	
	public BatchWriteCompletionHandler(AioListener aioListener, AsynchronousSocketChannel socketChannel, BufferAllocator allocator, int maxMerge)
	{
		super(socketChannel, aioListener, allocator);
		this.maxMerge = maxMerge;
	}
	
	@Override
	protected void writeQueuedBuffer()
	{
		IoBuffer head = null;
		int maxMerge = this.maxMerge;
		int count = 0;
		IoBuffer buffer;
		while (count < maxMerge && (buffer = queue.poll()) != null)
		{
			if (head == null)
			{
				head = buffer;
			}
			else
			{
				head.put(buffer);
				buffer.free();
			}
		}
		entry.setIoBuffer(head);
		entry.setByteBuffer(head.readableByteBuffer());
		socketChannel.write(entry.getByteBuffer(), entry, this);
	}
}
