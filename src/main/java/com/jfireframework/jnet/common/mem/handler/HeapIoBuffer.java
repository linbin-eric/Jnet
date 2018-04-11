package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

class HeapIoBuffer extends IoBuffer
{
	private int			offset;
	protected byte[]	mem;
	
	@Override
	public String toString()
	{
		return "HeapHandler [readPosi=" + readPosi + ", writePosi=" + writePosi + ", capacity=" + capacity + ", offset=" + offset + "]";
	}
	
	@Override
	protected void _initialize(int offset, int capacity, Object mem)
	{
		if (mem instanceof byte[] == false)
		{
			throw new IllegalArgumentException();
		}
		this.mem = (byte[]) mem;
		this.offset = offset;
		readPosi = writePosi = offset;
	}
	
	@Override
	public byte get()
	{
		byte b = mem[readPosi];
		readPosi += 1;
		return b;
	}
	
	@Override
	public byte get(int posi)
	{
		return mem[offset + posi];
	}
	
	@Override
	public IoBuffer compact()
	{
		int length = remainRead();
		System.arraycopy(mem, readPosi, mem, offset, length);
		readPosi = offset;
		writePosi -= length;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		System.arraycopy(mem, readPosi, content, 0, content.length);
		readPosi += content.length;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		System.arraycopy(mem, readPosi, content, off, len);
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
				if (mem[i + l] != array[l])
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
		int i = (mem[readPosi] & 0xff) << 24;
		i = i | (mem[readPosi + 1] & 0xff) << 16;
		i = i | (mem[readPosi + 2] & 0xff) << 8;
		i = i | (mem[readPosi + 3] & 0xff);
		readPosi += 4;
		return i;
	}
	
	@Override
	public short readShort()
	{
		short s = (short) ((mem[readPosi] & 0xff) << 8);
		s = (short) (s | (mem[readPosi + 1] & 0xff));
		readPosi += 2;
		return s;
	}
	
	@Override
	public long readLong()
	{
		long l = ((long) mem[readPosi] << 56) //
		        | (((long) mem[readPosi + 1] & 0xff) << 48) //
		        | (((long) mem[readPosi + 2] & 0xff) << 40)//
		        | (((long) mem[readPosi + 3] & 0xff) << 32) //
		        | (((long) mem[readPosi + 4] & 0xff) << 24) //
		        | (((long) mem[readPosi + 5] & 0xff) << 16) //
		        | (((long) mem[readPosi + 6] & 0xff) << 8) //
		        | (((long) mem[readPosi + 7] & 0xff));
		readPosi += 8;
		return l;
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		cachedByteBuffer = ByteBuffer.wrap(mem, readPosi, remainRead());
		return cachedByteBuffer;
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
		byte[] expansionMem = expansionTransit.mem;
		int expansionOffset = expansionTransit.offset;
		System.arraycopy(mem, offset, expansionMem, expansionOffset, length);
		expansionTransit.writePosi = length + expansionTransit.offset;
		expansionTransit.readPosi = expansionTransit.offset + (readPosi - offset);
		//
		byte[] tmpMem = mem;
		mem = expansionTransit.mem;
		expansionTransit.mem = tmpMem;
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
	
	protected void _destoryMem()
	{
		mem = null;
	}
	
	@Override
	protected void _put(byte b)
	{
		mem[writePosi] = b;
		writePosi += 1;
	}
	
	@Override
	protected void _put(byte b, int posi)
	{
		mem[offset + posi] = b;
	}
	
	@Override
	protected void _put(byte[] content)
	{
		System.arraycopy(content, 0, mem, writePosi, content.length);
		writePosi += content.length;
	}
	
	@Override
	protected void _put(byte[] content, int off, int len)
	{
		System.arraycopy(content, off, mem, writePosi, len);
		writePosi += len;
	}
	
	@Override
	protected void _put(IoBuffer src, int len)
	{
		if (src instanceof HeapIoBuffer)
		{
			HeapIoBuffer target = (HeapIoBuffer) src;
			System.arraycopy(target.mem, target.readPosi, mem, writePosi, len);
			writePosi += len;
		}
		else if (src instanceof DirectIoBuffer)
		{
			DirectIoBuffer source = (DirectIoBuffer) src;
			source.changeToRead();
			source.mem.get(mem, writePosi, len);
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
		mem[off] = (byte) (i >> 24);
		mem[off + 1] = (byte) (i >> 16);
		mem[off + 2] = (byte) (i >> 8);
		mem[off + 3] = (byte) (i);
	}
	
	@Override
	protected void _writeShort(short s, int off)
	{
		off += this.offset;
		mem[off] = (byte) (s >> 8);
		mem[off + 1] = (byte) (s);
	}
	
	@Override
	protected void _writeLong(long l, int off)
	{
		off += offset;
		mem[off] = (byte) (l >> 56);
		mem[off + 1] = (byte) (l >> 48);
		mem[off + 2] = (byte) (l >> 40);
		mem[off + 3] = (byte) (l >> 32);
		mem[off + 4] = (byte) (l >> 24);
		mem[off + 5] = (byte) (l >> 16);
		mem[off + 6] = (byte) (l >> 8);
		mem[off + 7] = (byte) (l);
	}
	
	@Override
	protected void _writeInt(int i)
	{
		mem[writePosi] = (byte) (i >> 24);
		mem[writePosi + 1] = (byte) (i >> 16);
		mem[writePosi + 2] = (byte) (i >> 8);
		mem[writePosi + 3] = (byte) (i);
		writePosi += 4;
	}
	
	@Override
	protected void _writeShort(short s)
	{
		mem[writePosi] = (byte) (s >> 8);
		mem[writePosi + 1] = (byte) (s);
		writePosi += 2;
	}
	
	@Override
	protected void _writeLong(long l)
	{
		mem[writePosi] = (byte) (l >> 56);
		mem[writePosi + 1] = (byte) (l >> 48);
		mem[writePosi + 2] = (byte) (l >> 40);
		mem[writePosi + 3] = (byte) (l >> 32);
		mem[writePosi + 4] = (byte) (l >> 24);
		mem[writePosi + 5] = (byte) (l >> 16);
		mem[writePosi + 6] = (byte) (l >> 8);
		mem[writePosi + 7] = (byte) (l);
		writePosi += 8;
	}
	
}
