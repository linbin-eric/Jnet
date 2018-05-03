package com.jfireframework.jnet.common.buffer;

class PooledHeapArchon extends Archon
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
	
	@Override
	protected void expansionForHugeCapacity(PooledIoBuffer buffer, int newSize)
	{
		Chunk predChunk = buffer.chunk;
		int predIndex = buffer.index;
		byte[] newArray = new byte[newSize];
		System.arraycopy(buffer.array, buffer.arrayOffset, newArray, 0, buffer.writePosi);
		buffer.array = newArray;
		buffer.arrayOffset = 0;
		buffer.capacity = newSize;
		if (buffer.chunk() != null)
		{
			recycle(predChunk, predIndex);
		}
		buffer.chunk = null;
		buffer.index = -1;
		buffer.internalByteBuffer = null;
	}
	
}
