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
	protected Archon<T>		archon;
	
	@Override
	public void initialize(int off, int capacity, T mem, int index, Chunk<T> chunk, Archon<T> archon)
	{
		this.capacity = capacity;
		this.archon = archon;
		this.mem = mem;
		this.index = index;
		this.chunk = chunk;
		cachedByteBuffer = null;
		_initialize(off, capacity, mem, index, chunk, archon);
	}
	
	public abstract void _initialize(int off, int capacity, T mem, int index, Chunk<T> chunk, Archon<T> archon);
	
	/**
	 * 拷贝src中的数据到自身中。执行该操作时，自身应该处于初始化的状态。该拷贝方法会复制的信息包含:<br/>
	 * 1. 从off到writePosi的所有数据<br/>
	 * 2. src的相对readPosi <br/>
	 * 3. src的相对writePosi <br/>
	 * 
	 * @param src
	 */
	public abstract void copy(AbstractHandler<T> src);
	
	/**
	 * 将src中的内容替换到自身中。该替换方法会替换的信息包含:<br/>
	 * 1. mem <br/>
	 * 2. chunk <br/>
	 * 3. archon <br/>
	 * 4. readPosi <br/>
	 * 5. writePosi <br/>
	 * 6. off <br/>
	 * 7. capacity <br/>
	 * 8. index <br/>
	 * 
	 * @param src
	 */
	public abstract void replace(AbstractHandler<T> src);
	
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
