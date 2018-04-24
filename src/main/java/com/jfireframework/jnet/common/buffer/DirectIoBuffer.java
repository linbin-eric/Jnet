package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

class DirectIoBuffer extends AbstractIoBuffer<ByteBuffer>
{
	
	@Override
	public byte get()
	{
		byte b = memory.get(readPosi);
		readPosi += 1;
		return b;
	}
	
	@Override
	public byte get(int posi)
	{
		byte b = memory.get(posi + offset);
		return b;
	}
	
	@Override
	public IoBuffer compact()
	{
		ByteBuffer duplicate = memory.duplicate();
		duplicate.limit(offset + capacity).position(offset);
		ByteBuffer slice = duplicate.slice();
		slice.limit(writePosi - offset).position(readPosi - offset);
		slice.compact();
		writePosi -= readPosi;
		readPosi = offset;
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		return get(content, 0, content.length);
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		ByteBuffer buffer = internalByteBuffer();
		buffer.limit(writePosi).position(readPosi);
		buffer.get(content, off, len);
		readPosi += len;
		return this;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		for (int i = readPosi; i < writePosi; i++)
		{
			boolean match = true;
			for (int l = 0; i < array.length; l++)
			{
				if (memory.get(i + l) != array[l])
				{
					match = false;
					break;
				}
			}
			if (match)
			{
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int readInt()
	{
		int result = memory.getInt(readPosi);
		readPosi += 4;
		return result;
	}
	
	@Override
	public short readShort()
	{
		short s = memory.getShort(readPosi);
		readPosi += 2;
		return s;
	}
	
	@Override
	public long readLong()
	{
		long l = memory.getLong(readPosi);
		readPosi += 8;
		return l;
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		ByteBuffer buffer = internalByteBuffer();
		buffer.limit(writePosi).position(readPosi);
		return buffer;
	}
	
	@Override
	public void expansion(IoBuffer transit)
	{
		if (transit instanceof DirectIoBuffer == false)
		{
			throw new IllegalArgumentException();
		}
		DirectIoBuffer expansionTransit = (DirectIoBuffer) transit;
		if (expansionTransit.writePosi != expansionTransit.offset || expansionTransit.readPosi != expansionTransit.offset)
		{
			throw new IllegalArgumentException();
		}
		//复制从offset到writePosi的所有内容到transitBuffer
		ByteBuffer transitBuffer = expansionTransit.internalByteBuffer();
		transitBuffer.position(expansionTransit.offset).limit(expansionTransit.offset + expansionTransit.capacity);
		ByteBuffer src = internalByteBuffer();
		src.limit(offset+writePosi).position(offset);
		transitBuffer.put(src);
		/**Chunk***/
		Chunk tmpChunk = chunk;
		chunk = expansionTransit.chunk;
		expansionTransit.chunk = tmpChunk;
		/**Index***/
		int tmpIndex = index;
		index = expansionTransit.index;
		expansionTransit.index = tmpIndex;
		/**capacity***/
		capacity = expansionTransit.capacity;
		//读写坐标的更新需要先于本地offset的更新。因为更新依赖相对数据，所以不能在这之前更新offset。
		/**readPosi***/
		readPosi = expansionTransit.offset +getReadPosi();
		/**writePosi***/
		writePosi =expansionTransit.offset+getWritePosi();
		/**offset***/
		offset = expansionTransit.offset;
	}
	
	@Override
	protected void _put(byte b)
	{
		changeToWrite();
		mem.put(b);
		writePosi += 1;
	}
	
	@Override
	protected void _put(byte b, int posi)
	{
		changeToWrite();
		mem.put(posi, b);
	}
	
	@Override
	protected void _put(byte[] content)
	{
		changeToWrite();
		mem.put(content);
		writePosi += content.length;
	}
	
	@Override
	protected void _put(byte[] content, int off, int len)
	{
		changeToWrite();
		mem.put(content, off, len);
		writePosi += len;
	}
	
	@Override
	protected void _put(IoBuffer handler, int len)
	{
		if (handler instanceof HeapIoBuffer)
		{
			byte[] src = ((HeapIoBuffer) handler).mem;
			// 需要使用直接的readPosi数据，通过方法获得都是相对数据
			changeToWrite();
			mem.put(src, ((HeapIoBuffer) handler).readPosi, len);
			writePosi += len;
		}
		else if (handler instanceof DirectIoBuffer)
		{
			changeToWrite();
			DirectIoBuffer target = (DirectIoBuffer) handler;
			target.changeToRead();
			target.mem.limit(target.mem.position() + len);
			mem.put(target.mem);
			writePosi += len;
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	protected void _writeInt(int i, int off)
	{
		changeToWrite();
		mem.putInt(off, i);
	}
	
	@Override
	protected void _writeShort(short s, int off)
	{
		changeToWrite();
		mem.putShort(off, s);
	}
	
	@Override
	protected void _writeLong(long l, int off)
	{
		changeToWrite();
		mem.putLong(off, l);
	}
	
	@Override
	protected void _writeInt(int i)
	{
		changeToWrite();
		mem.putInt(i);
		writePosi += 4;
	}
	
	@Override
	protected void _writeShort(short s)
	{
		changeToWrite();
		mem.putShort(s);
		writePosi += 2;
	}
	
	@Override
	protected void _writeLong(long l)
	{
		changeToWrite();
		mem.putLong(l);
		writePosi += 8;
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	@Override
	protected ByteBuffer internalByteBuffer()
	{
		if (internalByteBuffer == null)
		{
			internalByteBuffer = memory.duplicate();
		}
		return internalByteBuffer;
	}
	
}
