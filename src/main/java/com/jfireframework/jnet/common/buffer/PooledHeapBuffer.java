package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class PooledHeapBuffer extends PooledIoBuffer
{
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
	@Override
	protected void _put(int posi, byte b)
	{
		array[actualArrayOffset(posi)] = b;
	}
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		checkBounds(content, off, len);
		int posi = nextWritePosi(len);
		System.arraycopy(content, off, array, actualArrayOffset(posi), len);
		return this;
	}
	
	@Override
	public void _put(int posi, IoBuffer buffer, int len)
	{
		if (buffer.isDirect())
		{
			AbstractIoBuffer src = (AbstractIoBuffer) buffer;
			Bits.copyToArray(src.actualAddress(src.readPosi), array, actualArrayOffset(posi), len);
		}
		else
		{
			AbstractIoBuffer src = (AbstractIoBuffer) buffer;
			System.arraycopy(src.array, src.actualArrayOffset(src.readPosi), array, actualArrayOffset(posi), len);
		}
	}
	
	@Override
	protected void _putInt(int posi, int value)
	{
		posi = actualArrayOffset(posi);
		Bits.putInt(array, posi, value);
	}
	
	@Override
	protected void _putShort(int posi, short value)
	{
		posi = actualArrayOffset(posi);
		Bits.putShort(array, posi, value);
	}
	
	@Override
	protected void _putLong(int posi, long value)
	{
		posi = actualArrayOffset(posi);
		Bits.putLong(array, posi, value);
	}
	
	@Override
	protected byte _get(int posi)
	{
		return array[actualArrayOffset(posi)];
	}
	
	@Override
	public IoBuffer compact()
	{
		if (readPosi == 0)
		{
			return this;
		}
		int length = writePosi - readPosi;
		System.arraycopy(array, actualArrayOffset(readPosi), array, actualArrayOffset(0), length);
		readPosi = 0;
		writePosi = length;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		checkBounds(content, off, len);
		if (remainRead() < len)
		{
			throw new IllegalArgumentException();
		}
		int r = nextReadPosi(len);
		System.arraycopy(array, actualArrayOffset(r), content, off, len);
		return this;
	}
	
	@Override
	protected final int _getInt(int posi)
	{
		posi = actualArrayOffset(posi);
		return Bits.getInt(array, posi);
	}
	
	@Override
	protected short _getShort(int posi)
	{
		posi = actualArrayOffset(posi);
		return Bits.getShort(array, posi);
	}
	
	@Override
	protected long _getLong(int posi)
	{
		posi = actualArrayOffset(posi);
		return Bits.getLong(array, posi);
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		if (internalByteBuffer == null)
		{
			internalByteBuffer = ByteBuffer.wrap(array, actualArrayOffset(0), capacity);
		}
		internalByteBuffer.limit(writePosi).position(readPosi);
		return internalByteBuffer;
	}
}
