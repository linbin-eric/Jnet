package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;

public class DirectChunk extends Chunk<ByteBuffer>
{
	
	public DirectChunk(int maxLevel, int pageSize)
	{
		super(maxLevel, pageSize);
	}
	
	@Override
	ByteBuffer initializeMemory()
	{
		return ByteBuffer.allocateDirect(chunkSize);
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
}
