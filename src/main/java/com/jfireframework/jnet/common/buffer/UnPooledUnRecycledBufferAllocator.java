package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class UnPooledUnRecycledBufferAllocator implements BufferAllocator
{
	public static UnPooledUnRecycledBufferAllocator DEFAULT = new UnPooledUnRecycledBufferAllocator();
	
	@Override
	public IoBuffer heapBuffer(int initializeCapacity)
	{
		UnPooledBuffer<byte[]> buffer = new UnPooledHeapBuffer();
		buffer.init(new byte[initializeCapacity], initializeCapacity);
		return buffer;
	}
	
	@Override
	public IoBuffer directBuffer(int initializeCapacity)
	{
		UnPooledBuffer<ByteBuffer> buffer = new UnPooledDirectBuffer();
		buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity);
		return buffer;
	}
	
}
