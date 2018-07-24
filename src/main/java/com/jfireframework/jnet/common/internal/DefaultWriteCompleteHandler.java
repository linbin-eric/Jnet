package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import com.jfireframework.baseutil.concurrent.MPSCArrayQueue;
import com.jfireframework.baseutil.concurrent.MPSCLinkedQueue;
import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.BackPressureMode;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultWriteCompleteHandler implements WriteCompletionHandler
{
	protected static final long					STATE_OFFSET	= UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
	protected static final int					SPIN_THRESHOLD	= 1 << 7;
	protected static final int					WORK			= 1;
	protected static final int					IDLE			= 2;
	// 终止状态位，也就是负数标识位
	protected static final int					TERMINATION		= 3;
	// 终止状态。进入该状态后，不再继续使用
	////////////////////////////////////////////////////////////
	protected volatile int						state			= IDLE;
	protected Queue<IoBuffer>					queue;
	protected final WriteEntry					entry			= new WriteEntry();
	protected final AsynchronousSocketChannel	socketChannel;
	protected final BufferAllocator				allocator;
	protected final AioListener					aioListener;
	protected final int							maxWriteBytes;
	
	public DefaultWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes, BackPressureMode backPressureMode)
	{
		this.socketChannel = socketChannel;
		this.allocator = allocator;
		this.aioListener = aioListener;
		this.maxWriteBytes = Math.max(1, maxWriteBytes);
		queue = backPressureMode.isEnable() ? new MPSCArrayQueue<IoBuffer>(backPressureMode.getQueueCapacity()) : new MPSCLinkedQueue<IoBuffer>();
	}
	
	protected void rest()
	{
		state = IDLE;
		if (queue.isEmpty() == false)
		{
			int now = state;
			if (now == IDLE && changeToWork())
			{
				if (queue.isEmpty() == false)
				{
					writeQueuedBuffer();
				}
				else
				{
					rest();
				}
			}
		}
	}
	
	protected boolean changeToWork()
	{
		return UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
	}
	
	@Override
	public void completed(Integer result, WriteEntry entry)
	{
		if (aioListener != null)
		{
			try
			{
				aioListener.afterWrited(socketChannel, result);
			}
			catch (Throwable e)
			{
				;
			}
		}
		ByteBuffer byteBuffer = entry.getByteBuffer();
		if (byteBuffer.hasRemaining())
		{
			socketChannel.write(byteBuffer, entry, this);
			return;
		}
		entry.getIoBuffer().free();
		entry.clear();
		if (queue.isEmpty() == false)
		{
			writeQueuedBuffer();
			return;
		}
		for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
		{
			if (queue.isEmpty() == false)
			{
				writeQueuedBuffer();
				return;
			}
		}
		rest();
	}
	
	@Override
	public boolean offer(IoBuffer buf)
	{
		if (buf == null)
		{
			throw new NullPointerException();
		}
		if (queue.offer(buf) == false)
		{
			return false;
		}
		int now = state;
		if (now == TERMINATION)
		{
			throw new IllegalStateException("该通道已经处于关闭状态，无法执行写操作");
		}
		if (now == IDLE && changeToWork())
		{
			if (queue.isEmpty() == false)
			{
				writeQueuedBuffer();
			}
			else
			{
				rest();
			}
		}
		return true;
	}
	
	/**
	 * 从MPSCQueue中取得IoBuffer，并且执行写操作
	 */
	private void writeQueuedBuffer()
	{
		IoBuffer head = null;
		int maxBufferedCapacity = this.maxWriteBytes;
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
	
	@Override
	public void failed(Throwable exc, WriteEntry entry)
	{
		if (aioListener != null)
		{
			aioListener.catchException(exc, socketChannel);
		}
		state = TERMINATION;
		entry.getIoBuffer().free();
		entry.clear();
		while (queue.isEmpty() == false)
		{
			queue.poll().free();
		}
		try
		{
			socketChannel.close();
		}
		catch (IOException e)
		{
			;
		}
	}
	
}
