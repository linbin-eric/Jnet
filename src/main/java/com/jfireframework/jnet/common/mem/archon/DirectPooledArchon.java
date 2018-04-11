package com.jfireframework.jnet.common.mem.archon;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.DirectChunk;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;
import com.jfireframework.jnet.common.mem.handler.DirectIoBuffer;

public class DirectPooledArchon extends PooledArchon<ByteBuffer>
{
	
	public DirectPooledArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
		expansionIoBuffer = new DirectIoBufferHandler();
	}
	
	@Override
	protected void initHugeBucket(IoBuffer handler, int need)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(need);
		handler.initialize(0, need, byteBuffer, 0, null, null);
	}
	
	@Override
	protected Chunk newChunk(int maxLevel, int unit)
	{
		return new DirectChunk(maxLevel, unit);
	}
	
	class DirectIoBufferHandler extends DirectIoBuffer implements ExpansionIoBuffer
	{
		
		@Override
		public void clearForNextCall()
		{
			chunk = null;
			mem = null;
			index = -1;
			capacity = -1;
		}
		
	}
}
