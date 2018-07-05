package com.jfireframework.jnet.common.buffer;

public class HeapChunkList extends ChunkList<byte[]>
{
	
	public HeapChunkList(int minUsage, int maxUsage, ChunkList<byte[]> next, int chunkSize)
	{
		super(minUsage, maxUsage, next, chunkSize);
	}
	
}
