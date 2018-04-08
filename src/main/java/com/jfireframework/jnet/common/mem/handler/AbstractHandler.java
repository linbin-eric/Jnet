package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

public abstract class AbstractHandler<T> implements Handler<T>
{
	protected T				mem;
	protected int			index;
	protected Chunk<T>		chunk;
	protected ByteBuffer	cachedByteBuffer;
	
	@Override
	public Chunk<T> belong()
	{
		return chunk;
	}
	
	@Override
	public void destory()
	{
		mem = null;
		index = -1;
		chunk = null;
		cachedByteBuffer = null;
	}
	
	@Override
	public boolean isEnoughWrite(int size)
	{
		return remainWrite() >= size;
	}
	
	@Override
	public String toString()
	{
		return "Bucket [index=" + index + "]";
	}
	
	@Override
	public int getIndex()
	{
		return index;
	}
	
	@Override
	public ByteBuffer cachedByteBuffer()
	{
		if (cachedByteBuffer != null)
		{
			return cachedByteBuffer;
		}
		return byteBuffer();
	}
}
