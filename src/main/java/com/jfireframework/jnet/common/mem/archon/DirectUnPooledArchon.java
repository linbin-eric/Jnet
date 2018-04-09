package com.jfireframework.jnet.common.mem.archon;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.handler.Handler;

public class DirectUnPooledArchon extends UnPooledArchon<ByteBuffer>
{
	
	@Override
	public void apply(int need, Handler<ByteBuffer> handler)
	{
		handler.initialize(0, need, ByteBuffer.allocateDirect(need), 0, null, null);
	}

	@Override
	public void expansion(Handler<ByteBuffer> handler, int newSize)
	{
		handler.initialize(0, newSize, ByteBuffer.allocateDirect(newSize), 0, null, null);
	}
	
}
