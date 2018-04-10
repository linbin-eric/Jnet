package com.jfireframework.jnet.common.mem.chunk;

import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class HeapChunk extends Chunk<byte[]>
{
	
	public HeapChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected byte[] initializeMem(int capacity)
	{
		return new byte[capacity];
	}
	
	@Override
	protected void initHandler(Archon<byte[]> archon, IoBuffer<byte[]> handler, int index, int off, int len)
	{
		handler.initialize(off, len, mem, index, this, archon);
	}
	
}
