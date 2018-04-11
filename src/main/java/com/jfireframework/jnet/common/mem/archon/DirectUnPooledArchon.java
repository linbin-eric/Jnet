package com.jfireframework.jnet.common.mem.archon;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class DirectUnPooledArchon extends UnPooledArchon
{
	
	@Override
	public void apply(int need, IoBuffer buffer)
	{
		buffer.initialize(0, need, ByteBuffer.allocateDirect(need), 0, null, this);
	}
	
	@Override
	public void expansion(IoBuffer buffer, int newSize)
	{
		IoBuffer expansionIoBuffer = IoBuffer.directBuffer();
		apply(newSize, expansionIoBuffer);
		expansionIoBuffer.copyAndReplace(buffer);
		recycle(buffer);
		buffer.replace(expansionIoBuffer);
		expansionIoBuffer.destory();
	}
	
}
