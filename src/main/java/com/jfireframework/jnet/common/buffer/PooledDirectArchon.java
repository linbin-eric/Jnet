package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class PooledDirectArchon extends PooledArchon
{
	
	PooledDirectArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initHugeBuffer(PooledIoBuffer handler, int need)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(need);
		long address = DirectChunk.getAddress(byteBuffer);
		handler.setDirectIoBufferArgs(this, null, -1, address, 0, need);
	}
	
	@Override
	protected Chunk newChunk(int maxLevel, int unit)
	{
		return Chunk.newDirectChunk(maxLevel, unit);
	}
	
}
