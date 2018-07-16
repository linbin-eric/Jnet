package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import com.jfireframework.baseutil.concurrent.MPSCArrayQueue;
import com.jfireframework.baseutil.concurrent.MPSCLinkedQueue;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public abstract class AbstractWriteCompletionHandler implements WriteCompletionHandler
{
	protected static final Unsafe				unsafe			= ReflectUtil.getUnsafe();
	protected static final long					STATE_OFFSET	= UnsafeFieldAccess.getFieldOffset("state", AbstractWriteCompletionHandler.class);
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
	
	public AbstractWriteCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int queueCapacity)
	{
		this.socketChannel = socketChannel;
		this.allocator = allocator;
		this.aioListener = aioListener;
		queue = queueCapacity == 0 ? new MPSCLinkedQueue<IoBuffer>() : new MPSCArrayQueue<IoBuffer>(queueCapacity);
		
	}
	
	@Override
	public void offer(IoBuffer buf)
	{
		if (buf == null)
		{
			throw new NullPointerException();
		}
		if (queue.offer(buf) == false)
		{
			while (queue.offer(buf) == false)
			{
				Thread.yield();
			}
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
	
	/**
	 * 从MPSCQueue中取得IoBuffer，并且执行写操作
	 */
	protected abstract void writeQueuedBuffer();
	
	protected boolean changeToWork()
	{
		return unsafe.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
	}
	
	@Override
	public void failed(Throwable exc, WriteEntry entry)
	{
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
