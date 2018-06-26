package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;

public class DirectChunkList extends ChunkList<ByteBuffer>
{
	
	public DirectChunkList(int minUsage, int maxUsage, ChunkList<ByteBuffer> next, int chunkSize)
	{
		super(minUsage, maxUsage, next, chunkSize);
	}
	
}
