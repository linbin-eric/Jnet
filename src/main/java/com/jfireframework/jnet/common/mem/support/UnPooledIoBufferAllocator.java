package com.jfireframework.jnet.common.mem.support;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.DirectUnPooledArchon;
import com.jfireframework.jnet.common.mem.archon.HeapUnPooledArchon;
import com.jfireframework.jnet.common.mem.handler.AbstractIoBuffer;
import com.jfireframework.jnet.common.mem.handler.DirectIoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class UnPooledIoBufferAllocator implements IoBufferAllocator
{
	private Archon<byte[]>		heapArchon		= new HeapUnPooledArchon();
	private Archon<ByteBuffer>	directArchon	= new DirectUnPooledArchon();
	
	@Override
	public IoBuffer<?> allocate(int initSize)
	{
		AbstractIoBuffer<byte[]> buffer = new HeapIoBuffer();
		heapArchon.apply(initSize, buffer);
		return buffer;
	}
	
	@Override
	public IoBuffer<?> allocateDirect(int initSize)
	{
		AbstractIoBuffer<ByteBuffer> buffer = new DirectIoBuffer();
		directArchon.apply(initSize, buffer);
		return buffer;
	}
	
	@Override
	public void release(IoBuffer<?> ioBuffer)
	{
		;
	}
	
}
