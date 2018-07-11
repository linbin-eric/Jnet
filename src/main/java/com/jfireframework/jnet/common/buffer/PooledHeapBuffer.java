package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public class PooledHeapBuffer extends PooledBuffer<byte[]>
{
	
	@Override
	public IoBuffer compact()
	{
		System.arraycopy(memory, offset + readPosi, memory, offset, remainRead());
		writePosi -= readPosi;
		readPosi = 0;
		return this;
	}
	
	@Override
	public ByteBuffer readableByteBuffer()
	{
		return ByteBuffer.wrap(memory, readPosi + offset, remainRead());
	}
	
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
	int realPosi(int posi)
	{
		return posi + offset;
	}
	
	@Override
	void put0(int posi, byte value)
	{
		memory[realPosi(posi)] = value;
	}
	
	@Override
	void put0(byte[] content, int off, int len, int posi)
	{
		System.arraycopy(content, off, memory, realPosi(posi), len);
	}
	
	@Override
	void putInt0(int posi, int value)
	{
		Bits.putInt(memory, realPosi(posi), value);
	}
	
	@Override
	void putShort0(int posi, short value)
	{
		Bits.putShort(memory, realPosi(posi), value);
	}
	
	@Override
	void putLong0(int posi, long value)
	{
		Bits.putLong(memory, realPosi(posi), value);
	}
	
	@Override
	byte get0(int posi)
	{
		return memory[realPosi(posi)];
	}
	
	@Override
	void get0(byte[] content, int off, int len, int posi)
	{
		System.arraycopy(memory, realPosi(posi), content, off, len);
	}
	
	@Override
	int getInt0(int posi)
	{
		return Bits.getInt(memory, realPosi(posi));
	}
	
	@Override
	short getShort0(int posi)
	{
		return Bits.getShort(memory, realPosi(posi));
	}
	
	@Override
	long getLong0(int posi)
	{
		return Bits.getLong(memory, realPosi(posi));
	}
	
	@Override
	void put1(PooledHeapBuffer buffer, int len)
	{
		int posi = nextWritePosi(len);
		System.arraycopy(buffer.memory, buffer.getReadPosi() + buffer.offset, memory, realPosi(posi), len);
	}
	
	@Override
	void put2(PooledDirectBuffer buffer, int len)
	{
		int posi = nextWritePosi(len);
		Bits.copyToArray(buffer.addressPlusOffsetCache + buffer.readPosi, memory, realPosi(posi), len);
	}
	
}
