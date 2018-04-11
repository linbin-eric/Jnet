package com.jfireframework.jnet.common.mem.chunk;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class DirectChunk extends Chunk
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
	protected void initHandler(Archon archon, IoBuffer handler, int index, int off, int len)
	{
		mem.limit(off + len).position(off);
		ByteBuffer slice = mem.slice();
		// 恢复到初始状态
		mem.limit(capacity).position(0);
		handler.initialize(0, slice.capacity(), slice, index, this, archon);
	}
	
}
