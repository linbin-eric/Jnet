package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.Handler;

public class HeapUnPooledArchon extends UnPooledArchon<byte[]>
{
	
	@Override
	public void apply(int need, Handler<byte[]> handler)
	{
		handler.initialize(0, need, new byte[need], 0, null, null);
	}
}
