package com.jfireframework.jnet.common.mem.support;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.DirectUnPooledArchon;
import com.jfireframework.jnet.common.mem.archon.HeapUnPooledArchon;
import com.jfireframework.jnet.common.mem.buffer.AbstractIoBuffer;
import com.jfireframework.jnet.common.mem.buffer.DirectIoBuffer;
import com.jfireframework.jnet.common.mem.buffer.HeapIoBuffer;
import com.jfireframework.jnet.common.mem.buffer.IoBuffer;
import com.jfireframework.jnet.common.mem.handler.DirectHandler;
import com.jfireframework.jnet.common.mem.handler.HeapHandler;

public class UnPooledIoBufferAllocator implements IoBufferAllocator
{
	private Archon<byte[]>		heapArchon		= new HeapUnPooledArchon();
	private Archon<ByteBuffer>	directArchon	= new DirectUnPooledArchon();
	
	@Override
	public IoBuffer allocate(int initSize)
	{
		AbstractIoBuffer<byte[]> buffer = new HeapIoBuffer();
		buffer.initialize(heapArchon, new HeapHandler(), initSize, new HeapHandler());
		return buffer;
	}
	
	@Override
	public IoBuffer allocateDirect(int initSize)
	{
		AbstractIoBuffer<ByteBuffer> buffer = new DirectIoBuffer();
		buffer.initialize(directArchon, new DirectHandler(), initSize, new DirectHandler());
		return buffer;
	}
	
	@Override
	public void release(IoBuffer ioBuffer)
	{
		;
	}
	
}
