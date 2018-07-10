package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.recycler.Recycler;

public class UnPooledRecycledBufferAllocator implements BufferAllocator
{
	public static UnPooledRecycledBufferAllocator	DEFAULT					= new UnPooledRecycledBufferAllocator();
	private Recycler<UnPooledHeapBuffer>			unPooledHeapBuffers		= new Recycler<UnPooledHeapBuffer>() {
																				protected UnPooledHeapBuffer newObject(RecycleHandler handler)
																				{
																					UnPooledHeapBuffer buffer = new UnPooledHeapBuffer();
																					buffer.recycleHandler = handler;
																					return buffer;
																				};
																			};
	private Recycler<UnPooledDirectBuffer>			unPooledDirectBuffers	= new Recycler<UnPooledDirectBuffer>() {
																				protected UnPooledDirectBuffer newObject(RecycleHandler handler)
																				{
																					UnPooledDirectBuffer buffer = new UnPooledDirectBuffer();
																					buffer.recycleHandler = handler;
																					return buffer;
																				};
																			};
	private boolean									preferDirect			= true;
	
	public UnPooledRecycledBufferAllocator()
	{
		this(true);
	}
	
	public UnPooledRecycledBufferAllocator(boolean preferDirect)
	{
		this.preferDirect = preferDirect;
	}
	
	@Override
	public IoBuffer heapBuffer(int initializeCapacity)
	{
		UnPooledBuffer<byte[]> buffer = unPooledHeapBuffers.get();
		buffer.init(new byte[initializeCapacity], initializeCapacity);
		return buffer;
	}
	
	@Override
	public IoBuffer directBuffer(int initializeCapacity)
	{
		UnPooledBuffer<ByteBuffer> buffer = unPooledDirectBuffers.get();
		buffer.init(ByteBuffer.allocateDirect(initializeCapacity), initializeCapacity);
		return buffer;
	}
	
	@Override
	public IoBuffer ioBuffer(int initializeCapacity)
	{
		if (preferDirect)
		{
			return directBuffer(initializeCapacity);
		}
		else
		{
			return heapBuffer(initializeCapacity);
		}
	}
	
	@Override
	public IoBuffer ioBuffer(int initializeCapacity, boolean direct)
	{
		if (direct)
		{
			return directBuffer(initializeCapacity);
		}
		else
		{
			return heapBuffer(initializeCapacity);
		}
	}
}
