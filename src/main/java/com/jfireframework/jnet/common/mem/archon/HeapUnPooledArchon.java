package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class HeapUnPooledArchon extends UnPooledArchon
{
	
	@Override
	public void apply(int need, IoBuffer handler)
	{
		handler.initialize(0, need, new byte[need], 0, null, this);
	}
	
	@Override
	public void expansion(IoBuffer handler, int newSize)
	{
		IoBuffer expansionIoBuffer = IoBuffer.heapIoBuffer();
		apply(newSize, expansionIoBuffer);
		expansionIoBuffer.copyAndReplace(handler);
		recycle(handler);
		handler.replace(expansionIoBuffer);
		expansionIoBuffer.destory();
		
	}
}
