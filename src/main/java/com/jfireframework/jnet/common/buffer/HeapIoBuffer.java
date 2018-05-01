package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class HeapIoBuffer extends PooledIoBuffer<byte[]>
{
	
	@Override
	public byte get()
	{
		byte b = memory[readPosi];
		readPosi += 1;
		return b;
	}
	
	@Override
	public byte get(int posi)
	{
		return memory[offset + posi];
	}
	
	@Override
	public IoBuffer compact()
	{
		int length = remainRead();
		System.arraycopy(memory, readPosi, memory, offset, length);
		readPosi = offset;
		writePosi -= length;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		System.arraycopy(memory, readPosi, content, 0, content.length);
		readPosi += content.length;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		System.arraycopy(memory, readPosi, content, off, len);
		readPosi += len;
		return this;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		for (int i = readPosi; i < writePosi; i++)
		{
			boolean match = true;
			for (int l = 0; l < array.length; l++)
			{
				if (memory[i + l] != array[l])
				{
					match = false;
					break;
				}
			}
			if (match)
			{
				return i - offset;
			}
		}
		return -1;
	}
	
	@Override
	public int readInt()
	{
		int i = (memory[readPosi] & 0xff) << 24;
		i = i | (memory[readPosi + 1] & 0xff) << 16;
		i = i | (memory[readPosi + 2] & 0xff) << 8;
		i = i | (memory[readPosi + 3] & 0xff);
		readPosi += 4;
		return i;
	}
	
	@Override
	public short readShort()
	{
		short s = (short) ((memory[readPosi] & 0xff) << 8);
		s = (short) (s | (memory[readPosi + 1] & 0xff));
		readPosi += 2;
		return s;
	}
	
	@Override
	public long readLong()
	{
		long l = ((long) memory[readPosi] << 56) //
		        | (((long) memory[readPosi + 1] & 0xff) << 48) //
		        | (((long) memory[readPosi + 2] & 0xff) << 40)//
		        | (((long) memory[readPosi + 3] & 0xff) << 32) //
		        | (((long) memory[readPosi + 4] & 0xff) << 24) //
		        | (((long) memory[readPosi + 5] & 0xff) << 16) //
		        | (((long) memory[readPosi + 6] & 0xff) << 8) //
		        | (((long) memory[readPosi + 7] & 0xff));
		readPosi += 8;
		return l;
	}
	
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
	public void addReadPosi(int add)
	{
		readPosi += add;
	}
	
	@Override
	public void addWritePosi(int add)
	{
		writePosi += add;
	}
	
	@Override
	public void expansion(IoBuffer transit)
	{
		if (transit instanceof HeapIoBuffer == false)
		{
			throw new IllegalArgumentException();
		}
		HeapIoBuffer expansionTransit = (HeapIoBuffer) transit;
		if (expansionTransit.getWritePosi() != 0 || expansionTransit.getReadPosi() != 0)
		{
			throw new IllegalArgumentException();
		}
		int length = writePosi - offset;
		byte[] expansionMem = expansionTransit.memory;
		int expansionOffset = expansionTransit.offset;
		System.arraycopy(memory, offset, expansionMem, expansionOffset, length);
		expansionTransit.writePosi = length + expansionTransit.offset;
		expansionTransit.readPosi = expansionTransit.offset + (readPosi - offset);
		//
		byte[] tmpMem = memory;
		memory = expansionTransit.memory;
		expansionTransit.memory = tmpMem;
		Chunk tmpChunk = chunk;
		chunk = expansionTransit.chunk;
		expansionTransit.chunk = tmpChunk;
		int tmpCapacity = capacity;
		capacity = expansionTransit.capacity;
		expansionTransit.capacity = tmpCapacity;
		int tmpIndex = index;
		index = expansionTransit.index;
		expansionTransit.index = tmpIndex;
		int tmpOffset = offset;
		offset = expansionTransit.offset;
		expansionTransit.offset = tmpOffset;
		int tmpReadPosi = readPosi;
		readPosi = expansionTransit.readPosi;
		expansionTransit.readPosi = tmpReadPosi;
		int tmpWritePosi = writePosi;
		writePosi = expansionTransit.writePosi;
		expansionTransit.writePosi = tmpWritePosi;
	}
	
	@Override
	protected void _put(byte b)
	{
		memory[writePosi] = b;
		writePosi += 1;
	}
	
	@Override
	protected void _put(byte b, int posi)
	{
		memory[offset + posi] = b;
	}
	
	@Override
	protected void _put(byte[] content)
	{
		System.arraycopy(content, 0, memory, writePosi, content.length);
		writePosi += content.length;
	}
	
	@Override
	protected void _put(byte[] content, int off, int len)
	{
		System.arraycopy(content, off, memory, writePosi, len);
		writePosi += len;
	}
	
	@Override
	protected void _put(IoBuffer src, int len)
	{
		if (src instanceof HeapIoBuffer)
		{
			HeapIoBuffer target = (HeapIoBuffer) src;
			System.arraycopy(target.memory, target.readPosi, memory, writePosi, len);
			writePosi += len;
		}
		else if (src instanceof DirectIoBuffer)
		{
			DirectIoBuffer source = (DirectIoBuffer) src;
			source.changeToRead();
			source.mem.get(memory, writePosi, len);
			writePosi += len;
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	protected void _writeInt(int i, int off)
	{
		off += this.offset;
		memory[off] = (byte) (i >> 24);
		memory[off + 1] = (byte) (i >> 16);
		memory[off + 2] = (byte) (i >> 8);
		memory[off + 3] = (byte) (i);
	}
	
	@Override
	protected void _writeShort(short s, int off)
	{
		off += this.offset;
		memory[off] = (byte) (s >> 8);
		memory[off + 1] = (byte) (s);
	}
	
	@Override
	protected void _writeLong(long l, int off)
	{
		off += offset;
		memory[off] = (byte) (l >> 56);
		memory[off + 1] = (byte) (l >> 48);
		memory[off + 2] = (byte) (l >> 40);
		memory[off + 3] = (byte) (l >> 32);
		memory[off + 4] = (byte) (l >> 24);
		memory[off + 5] = (byte) (l >> 16);
		memory[off + 6] = (byte) (l >> 8);
		memory[off + 7] = (byte) (l);
	}
	
	@Override
	protected void _writeInt(int i)
	{
		memory[writePosi] = (byte) (i >> 24);
		memory[writePosi + 1] = (byte) (i >> 16);
		memory[writePosi + 2] = (byte) (i >> 8);
		memory[writePosi + 3] = (byte) (i);
		writePosi += 4;
	}
	
	@Override
	protected void _writeShort(short s)
	{
		memory[writePosi] = (byte) (s >> 8);
		memory[writePosi + 1] = (byte) (s);
		writePosi += 2;
	}
	
	@Override
	protected void _writeLong(long l)
	{
		memory[writePosi] = (byte) (l >> 56);
		memory[writePosi + 1] = (byte) (l >> 48);
		memory[writePosi + 2] = (byte) (l >> 40);
		memory[writePosi + 3] = (byte) (l >> 32);
		memory[writePosi + 4] = (byte) (l >> 24);
		memory[writePosi + 5] = (byte) (l >> 16);
		memory[writePosi + 6] = (byte) (l >> 8);
		memory[writePosi + 7] = (byte) (l);
		writePosi += 8;
	}
	
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
	@Override
	protected void ensureEnoughWrite(int needToWrite)
	{
		if (needToWrite < 0 || remainWrite() >= needToWrite)
		{
			return;
		}
		// 如果是从chunk分配的，则寻找archon进行扩容
		if (chunk != null)
		{
			chunk.archon.expansion(this, needToWrite + capacity);
		}
		else
		{
			DirectIoBuffer buffer = new DirectIoBuffer();
			buffer.initialize(0, needToWrite + capacity, ByteBuffer.allocateDirect(needToWrite + capacity), 0, null);
			expansion(buffer);
			buffer = null;
		}
	}
	
	@Override
	protected ByteBuffer internalByteBuffer()
	{
		if (internalByteBuffer == null)
		{
			internalByteBuffer = ByteBuffer.wrap(memory, offset, capacity);
		}
		return internalByteBuffer;
	}
	
}
