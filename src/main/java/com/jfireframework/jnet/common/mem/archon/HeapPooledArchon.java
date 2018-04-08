package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.Handler;

public class HeapPooledArchon extends PooledArchon<byte[]>
{
	
	public HeapPooledArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initHugeBucket(Handler<byte[]> handler, int need)
	{
		handler.initialize(0, need, new byte[need], 0, null);
	}
	
	@Override
	protected Chunk<byte[]> newChunk(int maxLevel, int unit)
	{
		HeapChunk heapChunk = new HeapChunk(maxLevel, unit);
		return heapChunk;
	}
	
}
