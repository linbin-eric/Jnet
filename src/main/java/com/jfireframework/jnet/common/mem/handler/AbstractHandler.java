package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

public abstract class AbstractHandler<T> implements Handler<T>
{
	protected T				mem;
	protected int			index;
	protected Chunk<T>		chunk;
	protected ByteBuffer	cachedByteBuffer;
	protected int			capacity;
	protected int			readPosi;
	protected int			writePosi;
	
	public void initialize(int off, int len, T mem, int index, Chunk<T> chunk, Archon<T> archon)
	{
		capacity = len;
		this.mem = mem;
		this.index = index;
		this.chunk = chunk;
		cachedByteBuffer = null;
		_initialize(off, len, mem, index, chunk, archon);
	}
	
	public abstract void _initialize(int off, int len, T mem, int index, Chunk<T> chunk, Archon<T> archon);
	
	
	protected void ensureCapacity(int size)
	{
		if (size <= 0)
		{
			return;
		}
		if (isEnoughWrite(size) == false)
		{
			archon.apply(handler.capacity() + size, expansionHandler);
			expansionHandler.put(handler);
			archon.recycle(handler);
			Handler<T> exchange = handler;
			handler = expansionHandler;
			expansionHandler = exchange;
			expansionHandler.destory();
		}
	}
	
	
	
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
