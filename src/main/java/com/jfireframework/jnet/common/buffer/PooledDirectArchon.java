package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class PooledDirectArchon extends PooledArchon
{
	
	PooledDirectArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initHugeBuffer(PooledIoBuffer handler, int need)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(need);
		long address = Bits.getAddress(byteBuffer);
		handler.setDirectIoBufferArgs(this, null, -1, address, 0, byteBuffer, need);
	}
	
	@Override
	protected Chunk newChunk(int maxLevel, int unit)
	{
		return Chunk.newDirectChunk(maxLevel, unit);
	}
	
	@Override
	protected void expansionForHugeCapacity(PooledIoBuffer buffer, int newSize)
	{
		Chunk predChunk = buffer.chunk();
		int predIndex = buffer.index;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(newSize);
		long destAddress = Bits.getAddress(byteBuffer);
		Bits.copyDirectMemory(buffer.address + buffer.addressOffset, destAddress, buffer.writePosi);
		buffer.address = destAddress;
		buffer.addressOffset = 0;
		buffer.capacity = newSize;
		buffer.addressBuffer = byteBuffer;
		if (buffer.chunk() != null)
		{
			recycle(predChunk, predIndex);
		}
		buffer.chunk = null;
		buffer.index = -1;
		buffer.internalByteBuffer = null;
	}
	
}
