package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public abstract class PooledIoBuffer extends AbstractIoBuffer
{
	protected Archon	archon;
	protected int			index;
	protected Chunk			chunk;
	
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
		internalByteBuffer = null;
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
	protected void setDirectIoBufferArgs(Archon archon, Chunk chunk, int index, long address, int addressOffset, ByteBuffer addressBuffer, int capacity)
	{
		if (isDirect() == false)
		{
			throw new IllegalArgumentException();
		}
		this.archon = archon;
		this.chunk = chunk;
		this.index = index;
		this.address = address;
		this.addressOffset = addressOffset;
		this.addressBuffer = addressBuffer;
		this.capacity = capacity;
		readPosi = writePosi = 0;
		internalByteBuffer = null;
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
		addressOffset = -1;
		addressBuffer = null;
		array = null;
		arrayOffset = -1;
		capacity = -1;
		internalByteBuffer = null;
	}
	
	public Chunk chunk()
	{
		return chunk;
	}
	
	public int index()
	{
		return index;
	}
	
	@Override
	public IoBuffer put(byte b)
	{
		int w = nextWritePosi(1);
		_put(w, b);
		return this;
	}
	
	/**
	 * 为writePosi增加长度。并且执行扩容检查。返回未增加之前的writePosi
	 * 
	 * @param len
	 * @return
	 */
	protected final int nextWritePosi(int len)
	{
		int w = writePosi;
		int newWritePosi = w + len;
		if (newWritePosi > capacity)
		{
			archon.expansion(this, newWritePosi);
		}
		writePosi = newWritePosi;
		return w;
	}
	
	protected final void ensureNewWritePosi(int newWritePosi)
	{
		if (newWritePosi > capacity)
		{
			// 扩容
			archon.expansion(this, newWritePosi);
		}
	}
	
	@Override
	public IoBuffer put(byte b, int posi)
	{
		writePosiCheck(posi, 1);
		_put(posi, b);
		return this;
	}
	
	protected final void writePosiCheck(int posi, int len)
	{
		if (posi < 0)
		{
			throw new IllegalArgumentException();
		}
		ensureNewWritePosi(posi + len);
	}
	
	@Override
	public IoBuffer put(byte[] content)
	{
		return put(content, 0, content.length);
	}
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		checkBounds(content, off, len);
		int end = off + len;
		for (int i = off; i < end; i++)
		{
			this.put(content[i]);
		}
		return this;
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer)
	{
		return put(buffer, buffer.remainRead());
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer, int len)
	{
		if (buffer.remainRead() < len)
		{
			throw new IllegalArgumentException();
		}
		int posi = nextWritePosi(len);
		_put(posi, buffer, len);
		return this;
	}
	
	@Override
	public IoBuffer putInt(int i)
	{
		int posi = nextWritePosi(4);
		_putInt(posi, i);
		return this;
	}
	
	@Override
	public IoBuffer writeInt(int i, int posi)
	{
		writePosiCheck(posi, 4);
		_putInt(posi, i);
		return this;
	}
	
	@Override
	public IoBuffer putShort(short s)
	{
		int posi = nextWritePosi(2);
		_putShort(posi, s);
		return this;
	}
	
	@Override
	public IoBuffer writeShort(short s, int posi)
	{
		writePosiCheck(posi, 2);
		_putShort(posi, s);
		return this;
	}
	
	@Override
	public IoBuffer putLong(long l)
	{
		int posi = nextWritePosi(8);
		_putLong(posi, l);
		return null;
	}
	
	@Override
	public IoBuffer writeLong(long l, int posi)
	{
		writePosiCheck(posi, 8);
		_putLong(posi, l);
		return this;
	}
	
	@Override
	public IoBuffer clearData()
	{
		writePosi = readPosi = 0;
		return this;
	}
	
	@Override
	public byte get()
	{
		byte b = _get(nextReadPosi(1));
		return b;
	}
	
	@Override
	public byte get(int posi)
	{
		readPosiCheck(posi, 1);
		return _get(posi);
	}
	
	protected final void readPosiCheck(int posi, int len)
	{
		if (posi < 0 || posi + len > writePosi)
		{
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		return get(content, 0, content.length);
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		checkBounds(content, off, len);
		if (remainRead() < len)
		{
			throw new IllegalArgumentException();
		}
		int end = off + len;
		for (int i = off; i < end; i++)
		{
			content[i] = get();
		}
		return this;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		for (int i = readPosi; i < writePosi; i++)
		{
			if (_get(i) == array[0])
			{
				int length = array.length;
				if (writePosi - i < length)
				{
					return -1;
				}
				boolean miss = false;
				for (int l = 0; l < length; l++)
				{
					if (_get(i + l) != array[l])
					{
						miss = true;
						break;
					}
				}
				if (miss == false)
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public int getInt()
	{
		int posi = nextReadPosi(4);
		return _getInt(posi);
	}
	
	@Override
	public short getShort()
	{
		int posi = nextReadPosi(2);
		return _getShort(posi);
	}
	
	@Override
	public long getLong()
	{
		int posi = nextReadPosi(8);
		return _getLong(posi);
	}
	
	public static PooledIoBuffer heapBuffer()
	{
		return new PooledHeapBuffer();
	}
	
	public static PooledIoBuffer directBuffer()
	{
		return new PooledDirectBuffer();
	}
	
}
