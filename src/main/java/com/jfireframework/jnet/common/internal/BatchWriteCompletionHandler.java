package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class BatchWriteCompletionHandler extends AbstractWriteCompletionHandler
{
	private final int maxBufferedCapacity;
	
	public BatchWriteCompletionHandler(AioListener aioListener, AsynchronousSocketChannel socketChannel, BufferAllocator allocator, int capacity, int maxBufferedCapacity)
	{
		super(socketChannel, aioListener, allocator, capacity);
		this.maxBufferedCapacity = maxBufferedCapacity;
	}
	
	public BatchWriteCompletionHandler(AioListener aioListener, AsynchronousSocketChannel socketChannel, BufferAllocator allocator, int capacity)
	{
		this(aioListener, socketChannel, allocator, capacity, 1024 * 1024);
	}
	
	@Override
	protected void writeQueuedBuffer()
	{
		IoBuffer head = null;
		int maxBufferedCapacity = this.maxBufferedCapacity;
		int count = 0;
		IoBuffer buffer;
		while (count < maxBufferedCapacity && (buffer = queue.poll()) != null)
		{
			count += buffer.remainRead();
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
