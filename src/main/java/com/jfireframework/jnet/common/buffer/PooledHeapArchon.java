package com.jfireframework.jnet.common.buffer;

class PooledHeapArchon extends PooledArchon
{
	
	PooledHeapArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initHugeBuffer(PooledIoBuffer handler, int need)
	{
		handler.setHeapIoBufferArgs(this, null, -1, new byte[need], 0, need);
	}
	
	@Override
	protected Chunk newChunk(int maxLevel, int unit)
	{
		return Chunk.newHeapChunk(maxLevel, unit);
	}
}
