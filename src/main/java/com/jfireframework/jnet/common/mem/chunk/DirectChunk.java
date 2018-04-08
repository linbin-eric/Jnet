package com.jfireframework.jnet.common.mem.chunk;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.handler.Handler;

public class DirectChunk extends Chunk<ByteBuffer>
{
	
	public DirectChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected ByteBuffer initializeMem(int capacity)
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
		buffer.limit(capacity).position(0);
		return buffer;
	}
	
	@Override
	protected void initHandler(Handler<ByteBuffer> handler, int index, int off, int len)
	{
		mem.limit(off + len).position(off);
		ByteBuffer slice = mem.slice();
		// 恢复到初始状态
		mem.limit(capacity).position(0);
		handler.initialize(0, slice.capacity(), slice, index, this);
	}
	
}
