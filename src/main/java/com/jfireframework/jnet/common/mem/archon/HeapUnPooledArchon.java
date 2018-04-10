package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.Handler;

public class HeapUnPooledArchon extends UnPooledArchon<byte[]>
{
	
	@Override
	public void apply(int need, Handler<byte[]> handler)
	{
		handler.initialize(0, need, new byte[need], 0, null, this);
	}

	@Override
	public void expansion(Handler<byte[]> handler, int newSize)
	{
		handler.initialize(0, newSize, new byte[newSize], 0, null, this);
	}
}
