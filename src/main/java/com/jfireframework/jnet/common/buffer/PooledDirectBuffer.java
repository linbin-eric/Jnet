package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class PooledDirectBuffer extends PooledIoBuffer
{
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	@Override
	protected void _put(int posi, byte b)
	{
		Bits.put(actualAddress(posi), b);
	}
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		if (len > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD)
		{
			checkBounds(content, off, len);
			int posi = nextWritePosi(len);
			Bits.copyFromByteArray(content, off, actualAddress(posi), len);
			return this;
		}
		else
		{
			return super.put(content, off, len);
		}
		
	}
	
	@Override
	protected void _put(int posi, IoBuffer buffer, int len)
	{
		if (buffer.isDirect())
		{
			AbstractIoBuffer src = (AbstractIoBuffer) buffer;
			Bits.copyDirectMemory(src.actualAddress(src.readPosi), actualAddress(posi), len);
		}
		else
		{
			AbstractIoBuffer src = (AbstractIoBuffer) buffer;
			Bits.copyFromByteArray(src.array, src.actualArrayOffset(src.readPosi), actualAddress(posi), len);
		}
	}
	
	@Override
	protected void _putInt(int posi, int value)
	{
		long a = actualAddress(posi);
		Bits.putInt(a, value);
	}
	
	@Override
	protected void _putShort(int posi, short value)
	{
		long a = actualAddress(posi);
		Bits.putShort(a, value);
	}
	
	@Override
	protected void _putLong(int posi, long value)
	{
		long a = actualAddress(posi);
		Bits.putLong(a, value);
	}
	
	@Override
	protected byte _get(int posi)
	{
		long a = actualAddress(posi);
		return Bits.get(a);
	}
	
	@Override
	public IoBuffer compact()
	{
		if (readPosi == 0)
		{
			return this;
		}
		int length = writePosi - readPosi;
		Bits.copyDirectMemory(actualAddress(readPosi), actualAddress(0), length);
		writePosi = length;
		readPosi = 0;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		if (len > Bits.JNI_COPY_TO_ARRAY_THRESHOLD)
		{
			checkBounds(content, off, len);
			if (remainRead() < len)
			{
				throw new IllegalArgumentException();
			}
			int posi = nextReadPosi(len);
			Bits.copyToArray(actualAddress(posi), content, off, len);
			return this;
		}
		else
		{
			return super.get(content, off, len);
		}
	}
	
	@Override
	protected int _getInt(int posi)
	{
		long a = actualAddress(posi);
		return Bits.getInt(a);
	}
	
	@Override
	protected short _getShort(int posi)
	{
		long a = actualAddress(posi);
		return Bits.getShort(a);
	}
	
	@Override
	protected long _getLong(int posi)
	{
		long a = actualAddress(posi);
		return Bits.getLong(a);
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		if (internalByteBuffer == null)
		{
			internalByteBuffer = addressBuffer.duplicate();
		}
		internalByteBuffer.limit(writePosi + addressOffset).position(readPosi + addressOffset);
		return internalByteBuffer;
	}
}
