package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class DirectChunk extends Chunk
{
	protected ByteBuffer mem;
	
	public DirectChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initializeMem(int capacity)
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
		buffer.limit(capacity).position(0);
		mem = buffer;
	}
	
	@Override
	protected void initHandler(Archon archon, PooledIoBuffer handler, int index, int off, int len)
	{
		mem.limit(off + len).position(off);
		ByteBuffer slice = mem.slice();
		// 恢复到初始状态
		mem.limit(capacity).position(0);
		handler.initialize(0, slice.capacity(), slice, index, this, archon);
	}
	
}
