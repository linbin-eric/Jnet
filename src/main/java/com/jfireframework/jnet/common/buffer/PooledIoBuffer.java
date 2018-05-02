package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public abstract class PooledIoBuffer implements IoBuffer
{
	protected Archon	archon;
	protected int		index;
	protected Chunk		chunk;
	// 当类为HeapIoBuffer时不为空
	protected byte[]	array;
	// 当类为HeapIoBuffer时为array中可用区域的起始偏移量
	protected int		arrayOffset;
	// 当类为DirectIoBuffer时不为-1.值是当前buffer可用的直接内存的起始位置
	protected long		address;
	// 当前buffer的容量字节数
	protected int		capacity;
	// 相对的读取坐标
	protected int		readPosi;
	// 相对的写入坐标
	protected int		writePosi;
	
	/**
	 * 如果类是HeapIoBuffer时调用该方法.
	 * 
	 * @param chunk 当前数据区域所属chunk
	 * @param index 当前数据区域在chunk中的坐标
	 * @param array 数据区域
	 * @param arrayOffset 该数据区域可以使用的起始偏移量
	 * @param capacity 该数据区域可以使用的大小
	 */
	protected void setHeapIoBufferArgs(Archon archon, Chunk chunk, int index, byte[] array, int arrayOffset, int capacity)
	{
		if (isDirect())
		{
			throw new IllegalArgumentException();
		}
		this.archon = archon;
		this.chunk = chunk;
		this.index = index;
		this.array = array;
		this.arrayOffset = arrayOffset;
		this.capacity = capacity;
		readPosi = writePosi = 0;
	}
	
	/**
	 * 如果类是DirectIoBUffer时调用该方法
	 * 
	 * @param chunk 当前数据区域所属chunk
	 * @param index 当前数据区域在chunk中的坐标
	 * @param address 当前数据区域在内存中的位置
	 * @param addressOffset 当前数据区域可以使用的起始偏移量
	 * @param capacity 该数据区域可以使用的大小
	 */
	protected void setDirectIoBufferArgs(Archon archon, Chunk chunk, int index, long address, int addressOffset, int capacity)
	{
		if (isDirect() == false)
		{
			throw new IllegalArgumentException();
		}
		this.archon = archon;
		this.chunk = chunk;
		this.index = index;
		this.address = address + addressOffset;
		this.capacity = capacity;
		readPosi = writePosi = 0;
	}
	
	/**
	 * 释放所有绑定的资源。将对应的对象设置为null以及将下标等设置为-1这种无意义值
	 */
	public void release()
	{
		archon = null;
		chunk = null;
		index = -1;
		address = -1;
		array = null;
		arrayOffset = -1;
		capacity = -1;
	}
	
	public Chunk chunk()
	{
		return chunk;
	}
	
	public int indexOfChunk()
	{
		return index;
	}
	
	@Override
	public int capacity()
	{
		return capacity;
	}
	
	@Override
	public IoBuffer put(byte b)
	{
		int newWritePosi = writePosi + 1;
		if (newWritePosi > capacity)
		{
			// 扩容
			archon.expansion(this, newWritePosi);
		}
		_put(writePosi, b);
		writePosi = newWritePosi;
		return this;
	}
	
	protected abstract void _put(int index, byte b);
	
	@Override
	public IoBuffer put(byte b, int posi)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer put(byte[] content)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer, int len)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeInt(int i)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeInt(int i, int posi)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeShort(short s, int posi)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeLong(long l, int posi)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeShort(short s)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer writeLong(long l)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getReadPosi()
	{
		return readPosi;
	}
	
	@Override
	public void setReadPosi(int readPosi)
	{
		this.readPosi = readPosi;
	}
	
	@Override
	public int getWritePosi()
	{
		return writePosi;
	}
	
	@Override
	public void setWritePosi(int writePosi)
	{
		this.writePosi = writePosi;
	}
	
	@Override
	public IoBuffer clearData()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public byte get()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public byte get(int posi)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int remainRead()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int remainWrite()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public IoBuffer compact()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void addReadPosi(int add)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addWritePosi(int add)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int readInt()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public short readShort()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long readLong()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isDirect()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
