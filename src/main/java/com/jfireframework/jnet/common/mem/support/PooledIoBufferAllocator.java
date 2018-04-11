package com.jfireframework.jnet.common.mem.support;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.DirectPooledArchon;
import com.jfireframework.jnet.common.mem.archon.HeapPooledArchon;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;
import com.jfireframework.jnet.common.mem.handler.DirectIoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class PooledIoBufferAllocator implements IoBufferAllocator
{
	
	private Archon<byte[]>[]		heapArchons;
	private Archon<ByteBuffer>[]	directArchons;
	
	private AtomicInteger			idx	= new AtomicInteger(0);
	private final int				mask;
	
	public PooledIoBufferAllocator()
	{
		mask = (1 << 3) - 1;
		heapArchons = new HeapPooledArchon[mask + 1];
		for (int i = 0; i < heapArchons.length; i++)
		{
			heapArchons[i] = new HeapPooledArchon(8, 128);
		}
		directArchons = new DirectPooledArchon[mask + 1];
		for (int i = 0; i < directArchons.length; i++)
		{
			directArchons[i] = new DirectPooledArchon(8, 128);
		}
	}
	
	@Override
	public IoBuffer<?> allocate(int initSize)
	{
		int index = idx.incrementAndGet() & mask;
		IoBuffer<byte[]> buffer = new HeapIoBuffer();
		heapArchons[index].apply(initSize, buffer);
		return buffer;
	}
	
	@Override
	public IoBuffer<?> allocateDirect(int initSize)
	{
		int index = idx.incrementAndGet() & mask;
		IoBuffer<ByteBuffer> buffer = new DirectIoBuffer();
		directArchons[index].apply(initSize, buffer);
		return buffer;
	}
	
	@Override
	public void release(IoBuffer<?> ioBuffer)
	{
		ioBuffer.release();
	}
	
}
