package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public class HeapPooledArchon extends PooledArchon<byte[]>
{
	
	public HeapPooledArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
		expansionIoBuffer = IoBuffer.heapIoBuffer();
	}
	
	@Override
	protected void initHugeBucket(IoBuffer handler, int need)
	{
		handler.initialize(0, need, new byte[need], 0, null, null);
	}
	
	@Override
	protected Chunk newChunk(int maxLevel, int unit)
	{
		return Chunk.newHeapChunk(maxLevel, unit);
	}
}
