package com.jfireframework.jnet.common.buffer2;

public class HeapChunk extends Chunk<byte[]>
{
	
	public HeapChunk(int maxLevel, int pageSize)
	{
		super(maxLevel, pageSize);
	}
	
	@Override
	byte[] initializeMemory(int size)
	{
		return new byte[size];
	}
	
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
}
