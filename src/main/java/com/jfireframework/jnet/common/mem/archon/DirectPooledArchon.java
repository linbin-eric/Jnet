package com.jfireframework.jnet.common.mem.archon;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.DirectChunk;
import com.jfireframework.jnet.common.mem.handler.Handler;

public class DirectPooledArchon extends PooledArchon<ByteBuffer>
{
	
	public DirectPooledArchon(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initHugeBucket(Handler<ByteBuffer> handler, int need)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(need);
		handler.initialize(0, need, byteBuffer, 0, null, null);
	}
	
	@Override
	protected Chunk<ByteBuffer> newChunk(int maxLevel, int unit)
	{
		return new DirectChunk(maxLevel, unit);
	}
	
}
