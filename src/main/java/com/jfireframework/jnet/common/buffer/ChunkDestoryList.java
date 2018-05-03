package com.jfireframework.jnet.common.buffer;

public class ChunkDestoryList extends ChunkList
{
	private int	chunkSum			= 0;
	private int	chunkSumtTreshol	= 1;
	
	public ChunkDestoryList(ChunkList next, int maxUsage, int minUsage, String name)
	{
		super(next, maxUsage, minUsage, name);
		this.name = "chunkDestoryList";
	}
	
	public void addChunk(Chunk chunk)
	{
		if (chunkSum >= chunkSumtTreshol)
		{
			return;
		}
		super.addChunk(chunk);
		if (chunk.parent == this)
		{
			chunkSum += 1;
		}
	}
	
	protected void removeFromCurrentList(Chunk node)
	{
		super.removeFromCurrentList(node);
		chunkSum -= 1;
	}
}
