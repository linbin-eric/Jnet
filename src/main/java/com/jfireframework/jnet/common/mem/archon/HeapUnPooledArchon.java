package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class HeapUnPooledArchon extends UnPooledArchon<byte[]>
{
	
	@Override
	public void apply(int need, IoBuffer<byte[]> handler)
	{
		handler.initialize(0, need, new byte[need], 0, null, this);
	}

	@Override
	public void expansion(IoBuffer<byte[]> handler, int newSize)
	{
		handler.initialize(0, newSize, new byte[newSize], 0, null, this);
	}
}
