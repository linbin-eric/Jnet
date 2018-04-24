package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.recycler.Recycler.RecycleHandler;

public abstract class AbstractIoBuffer<T> implements IoBuffer
{
	protected T					memory;
	protected int				index;
	protected Chunk				chunk;
	protected ByteBuffer		internalByteBuffer;
	protected int				offset;
	protected int				capacity;
	protected int				readPosi;
	protected int				writePosi;
	// 用于池化IoBuffer。如果没有此需求，该属性始终为空
	protected RecycleHandler	recycleHandler;
	
	public static IoBuffer heapIoBuffer()
	{
		return new HeapIoBuffer();
	}
	
	public static IoBuffer directBuffer()
	{
		return new DirectIoBuffer();
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(int offset, int capacity, Object memory, int index, Chunk chunk)
	{
		this.capacity = capacity;
		this.index = index;
		this.chunk = chunk;
		internalByteBuffer = null;
		this.offset = offset;
		this.memory = (T) memory;
		readPosi = writePosi = offset;
	}
	
	/**
	 * 通过一个初始化的Buffer：src进行扩容操作。具体流程如下<br/>
	 * 将自身数据拷贝到src中。该拷贝方法会复制的信息包含:<br/>
	 * 从off到writePosi的所有数据,相对readPosi,相对writePosi <br/>
	 * 将src中的数据和自身的数据进行互换。互换的信息包含：<br/>
	 * mem,chunk,archon,readPosi,writePosi,off,capacity,index <br/>
	 * 
	 * @param src
	 */
	protected abstract void expansion(IoBuffer src);
	
	public IoBuffer grow(int newSize)
	{
		ensureEnoughWrite(newSize - capacity);
		return this;
	}
	
	protected void ensureEnoughWrite(int needToWrite)
	{
		if (needToWrite < 0 || remainWrite() >= needToWrite)
		{
			return;
		}
		archon.expansion(this, capacity + needToWrite);
	}
	
	@Override
	public Chunk belong()
	{
		return chunk;
	}
	
	@Override
	public void release()
	{
		if (archon != null)
		{
			archon.recycle(this);
		}
	}
	
	@Override
	public void destory()
	{
		index = -1;
		chunk = null;
		archon = null;
		internalByteBuffer = null;
		memory = null;
		offset = capacity = writePosi = readPosi = 0;
	}
	
	@Override
	public String toString()
	{
		return "Handler [index=" + index + ", chunk=" + chunk + ", cachedByteBuffer=" + internalByteBuffer + ", capacity=" + capacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", archon=" + archon + "]";
	}
	
	@Override
	public int getIndex()
	{
		return index;
	}
	
	protected abstract ByteBuffer internalByteBuffer();
	
	@Override
	public int capacity()
	{
		return capacity;
	}
	
	@Override
	public IoBuffer put(byte b)
	{
		ensureEnoughWrite(1);
		_put(b);
		return this;
	}
	
	protected abstract void _put(byte b);
	
	@Override
	public IoBuffer put(byte b, int posi)
	{
		if (posi < 0)
		{
			throw new IllegalArgumentException();
		}
		ensureEnoughWrite(posi - getWritePosi());
		_put(b, posi);
		return this;
	}
	
	protected abstract void _put(byte b, int posi);
	
	@Override
	public IoBuffer put(byte[] content)
	{
		ensureEnoughWrite(content.length);
		_put(content);
		return this;
	}
	
	protected abstract void _put(byte[] content);
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		ensureEnoughWrite(off + len - getWritePosi());
		_put(content, off, len);
		return this;
	}
	
	protected abstract void _put(byte[] content, int off, int len);
	
	@Override
	public IoBuffer put(IoBuffer buffer)
	{
		return put(buffer, buffer.remainRead());
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer, int len)
	{
		ensureEnoughWrite(len);
		_put(buffer, len);
		return this;
	}
	
	protected abstract void _put(IoBuffer buffer, int len);
	
	@Override
	public IoBuffer writeInt(int i, int off)
	{
		ensureEnoughWrite(off + 4 - getWritePosi());
		_writeInt(i, off);
		return this;
	}
	
	protected abstract void _writeInt(int i, int off);
	
	@Override
	public IoBuffer writeShort(short s, int off)
	{
		ensureEnoughWrite(off + 2 - getWritePosi());
		_writeShort(s, off);
		return this;
	}
	
	protected abstract void _writeShort(short s, int off);
	
	@Override
	public IoBuffer writeLong(long l, int off)
	{
		ensureEnoughWrite(off + 8 - getWritePosi());
		_writeLong(l, off);
		return this;
	}
	
	protected abstract void _writeLong(long l, int off);
	
	@Override
	public IoBuffer writeInt(int i)
	{
		ensureEnoughWrite(4);
		_writeInt(i);
		return this;
	}
	
	protected abstract void _writeInt(int i);
	
	@Override
	public IoBuffer writeShort(short s)
	{
		ensureEnoughWrite(2);
		_writeShort(s);
		return this;
	}
	
	protected abstract void _writeShort(short s);
	
	@Override
	public IoBuffer writeLong(long l)
	{
		ensureEnoughWrite(8);
		_writeLong(l);
		return this;
	}
	
	protected abstract void _writeLong(long l);
	
	@Override
	public int getReadPosi()
	{
		return readPosi - offset;
	}
	
	@Override
	public void setReadPosi(int readPosi)
	{
		this.readPosi = readPosi + offset;
	}
	
	@Override
	public int getWritePosi()
	{
		return writePosi - offset;
	}
	
	@Override
	public void setWritePosi(int writePosi)
	{
		this.writePosi = writePosi + offset;
	}
	
	@Override
	public IoBuffer clearData()
	{
		readPosi = writePosi = offset;
		return this;
	}
	
	@Override
	public abstract byte get();
	
	@Override
	public abstract byte get(int posi);
	
	@Override
	public int remainRead()
	{
		return writePosi - readPosi;
	}
	
	@Override
	public int remainWrite()
	{
		return offset + capacity - writePosi;
	}
	
	@Override
	public abstract IoBuffer compact();
	
	@Override
	public abstract IoBuffer get(byte[] content);
	
	@Override
	public abstract IoBuffer get(byte[] content, int off, int len);
	
	@Override
	public void addReadPosi(int add)
	{
		readPosi += add;
	}
	
	@Override
	public void addWritePosi(int add)
	{
		writePosi += add;
	}
	
}
