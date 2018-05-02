package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.util.Bits;

class DirectChunk extends Chunk
{
	
	private ByteBuffer buffer;
	
	public DirectChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initializeMem(int capacity)
	{
		buffer = ByteBuffer.allocateDirect(capacity);
		address = Bits.getAddress(buffer);
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	@Override
	protected void initBuffer(PooledIoBuffer buffer, int index, int off, int capacity)
	{
		buffer.setDirectIoBufferArgs(archon, this, index, address, off, this.buffer, capacity);
	}
	
	@Override
	protected void expansionBuffer(PooledIoBuffer buffer, int index, int off, int capacity)
	{
		// TODO Auto-generated method stub
		
	}
	
}
