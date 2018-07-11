package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.util.PlatFormFunction;

public class PooledDirectBuffer extends PooledBuffer<ByteBuffer>
{
	long	address;
	long	addressPlusOffsetCache;
	
	@Override
	public void init(Chunk<ByteBuffer> chunk, int capacity, int offset, long handle, ThreadCache cache)
	{
		super.init(chunk, capacity, offset, handle, cache);
		address = PlatFormFunction.bytebufferOffsetAddress(memory);
		addressPlusOffsetCache = address + offset;
	}
	
	@Override
	public IoBuffer compact()
	{
		Bits.copyDirectMemory(addressPlusOffsetCache + readPosi, addressPlusOffsetCache, remainRead());
		writePosi -= readPosi;
		readPosi = 0;
		return this;
	}
	
	@Override
	public ByteBuffer readableByteBuffer()
	{
		ByteBuffer duplicate = memory.duplicate();
		duplicate.limit(offset + writePosi).position(offset + readPosi);
		return duplicate;
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	long realAddress(int posi)
	{
		return addressPlusOffsetCache + posi;
	}
	
	@Override
	void put0(int posi, byte value)
	{
		Bits.put(realAddress(posi), value);
	}
	
	@Override
	void put0(byte[] content, int off, int len, int posi)
	{
		Bits.copyFromByteArray(content, off, realAddress(posi), len);
	}
	
	@Override
	void putInt0(int posi, int value)
	{
		Bits.putInt(realAddress(posi), value);
	}
	
	@Override
	void putShort0(int posi, short value)
	{
		Bits.putShort(realAddress(posi), value);
	}
	
	@Override
	void putLong0(int posi, long value)
	{
		Bits.putLong(realAddress(posi), value);
	}
	
	@Override
	byte get0(int posi)
	{
		return Bits.get(realAddress(posi));
	}
	
	@Override
	void get0(byte[] content, int off, int len, int posi)
	{
		Bits.copyToArray(realAddress(posi), content, off, len);
	}
	
	@Override
	int getInt0(int posi)
	{
		return Bits.getInt(realAddress(posi));
	}
	
	@Override
	short getShort0(int posi)
	{
		return Bits.getShort(realAddress(posi));
	}
	
	@Override
	long getLong0(int posi)
	{
		return Bits.getLong(realAddress(posi));
	}
	
	@Override
	void put1(PooledHeapBuffer buffer, int len)
	{
		int posi = nextWritePosi(len);
		Bits.copyFromByteArray(buffer.memory, buffer.offset + buffer.readPosi, realAddress(posi), len);
	}
	
	@Override
	void put2(PooledDirectBuffer buffer, int len)
	{
		int posi = nextWritePosi(len);
		Bits.copyDirectMemory(buffer.addressPlusOffsetCache + readPosi, realAddress(posi), len);
	}
	
}
