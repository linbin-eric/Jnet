package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public abstract class AbstractIoBuffer implements IoBuffer
{
	// 当类为HeapIoBuffer时不为空
	protected byte[]		array;
	// 当类为HeapIoBuffer时为array中可用区域的起始偏移量
	protected int			arrayOffset;
	// 当类为DirectIoBuffer时不为-1.值是当前buffer可用的直接内存的起始位置
	protected long			address;
	// 当类为DirectIoBuffer时不为空。值是address关联的Buffer。
	protected ByteBuffer	addressBuffer;
	// 当类为DirectIoBuffer时不为-1.值是当前Address可用区域的偏移量
	protected int			addressOffset;
	// 当前buffer的容量字节数
	protected int			capacity;
	// 相对的读取坐标
	protected int			readPosi;
	// 相对的写入坐标
	protected int			writePosi;
	protected ByteBuffer	internalByteBuffer;
	
	/**
	 * 返回堆内内存数组posi偏移量的实际偏移量
	 * 
	 * @param posi
	 * @return
	 */
	protected final int actualArrayOffset(int posi)
	{
		return posi + arrayOffset;
	}
	
	/**
	 * 返回堆外内存posi地址的实际地址
	 * 
	 * @param posi
	 * @return
	 */
	protected final long actualAddress(int posi)
	{
		return address + addressOffset + posi;
	}
	
	@Override
	public int capacity()
	{
		return capacity;
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
	
	protected final void checkBounds(byte[] content, int off, int len)
	{
		if ((off | len | (len + off - content.length)) < 0)
		{
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 为readPosi增加length并且执行边界检查。返回未增加之前的readPosi
	 * 
	 * @param length
	 * @return
	 */
	protected final int nextReadPosi(int length)
	{
		int r = readPosi;
		int newReadPosi = r + length;
		if (newReadPosi > writePosi)
		{
			throw new IllegalArgumentException();
		}
		readPosi = newReadPosi;
		return r;
	}
	
	@Override
	public int remainRead()
	{
		return writePosi - readPosi;
	}
	
	@Override
	public int remainWrite()
	{
		return capacity - writePosi;
	}
	
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
	
	protected abstract void _put(int posi, IoBuffer buffer, int len);
	
	protected abstract void _put(int posi, byte b);
	
	protected abstract void _putInt(int posi, int value);
	
	protected abstract void _putShort(int posi, short value);
	
	protected abstract void _putLong(int posi, long value);
	
	protected abstract byte _get(int posi);
	
	protected abstract int _getInt(int posi);
	
	protected abstract short _getShort(int posi);
	
	protected abstract long _getLong(int posi);
}
