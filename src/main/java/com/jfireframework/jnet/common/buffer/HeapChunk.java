package com.jfireframework.jnet.common.buffer;

class HeapChunk extends Chunk
{
	
	public HeapChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initializeMem(int capacity)
	{
		array = new byte[capacity];
	}
	
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
	@Override
	protected void initBuffer(PooledIoBuffer buffer, int index, int off, int capacity)
	{
		buffer.setHeapIoBufferArgs(archon, this, index, array, off, capacity);
	}
	
	@Override
	protected void expansionBuffer(PooledIoBuffer buffer, int index, int off, int capacity)
	{
		// TODO Auto-generated method stub
		
	}
	
}
