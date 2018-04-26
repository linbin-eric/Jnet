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
		ByteBuffer duplicate = internalByteBuffer();
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
	public void expansion(IoBuffer transit)
	{
		if (transit.isDirect() == false)
		{
			throw new IllegalArgumentException("DirectioBuffer只能由DirectIoBuffer进行扩容");
		}
		DirectIoBuffer expansionTransit = (DirectIoBuffer) transit;
		if (expansionTransit.writePosi != expansionTransit.offset || expansionTransit.readPosi != expansionTransit.offset)
		{
			throw new IllegalArgumentException();
		}
		// 复制从offset到writePosi的所有内容到transitBuffer
		ByteBuffer transitBuffer = expansionTransit.internalByteBuffer();
		transitBuffer.position(expansionTransit.offset).limit(expansionTransit.offset + expansionTransit.capacity);
		ByteBuffer src = internalByteBuffer();
		src.limit(writePosi).position(readPosi);
		transitBuffer.put(src);
		/** Memory ***/
		ByteBuffer tmpMemory = memory;
		memory = expansionTransit.memory;
		expansionTransit.memory = tmpMemory;
		// 清空，确保下次使用的时候生成正确的。
		internalByteBuffer = null;
		/** Chunk ***/
		Chunk tmpChunk = chunk;
		chunk = expansionTransit.chunk;
		expansionTransit.chunk = tmpChunk;
		/** Index ***/
		int tmpIndex = index;
		index = expansionTransit.index;
		expansionTransit.index = tmpIndex;
		/** capacity ***/
		capacity = expansionTransit.capacity;
		// 读写坐标的更新需要先于本地offset的更新。因为更新依赖相对数据，所以不能在这之前更新offset。
		/** readPosi ***/
		readPosi = expansionTransit.offset + getReadPosi();
		/** writePosi ***/
		writePosi = expansionTransit.offset + getWritePosi();
		/** offset ***/
		offset = expansionTransit.offset;
	}
	
	@Override
	protected void _put(byte b)
	{
		memory.put(writePosi, b);
		writePosi += 1;
	}
	
	@Override
	protected void _put(byte b, int posi)
	{
		memory.put(posi + offset, b);
	}
	
	@Override
	protected void _put(byte[] content)
	{
		_put(content, 0, content.length);
	}
	
	@Override
	protected void _put(byte[] content, int off, int len)
	{
		ByteBuffer buffer = internalByteBuffer();
		buffer.limit(buffer.capacity()).position(writePosi);
		buffer.put(content, off, len);
		writePosi += len;
	}
	
	@Override
	protected void _put(IoBuffer src, int len)
	{
		if (src.isDirect() == false)
		{
			byte[] srcMemory = ((HeapIoBuffer) src).memory;
			_put(srcMemory, ((AbstractIoBuffer<?>) src).readPosi, src.remainRead());
		}
		else
		{
			ByteBuffer srcBuffer = src.byteBuffer();
			ByteBuffer destBuffer = internalByteBuffer();
			destBuffer.limit(destBuffer.capacity()).position(writePosi);
			destBuffer.put(srcBuffer);
			writePosi += len;
		}
	}
	
	@Override
	protected void _writeInt(int i, int off)
	{
		memory.putInt(off + this.offset, i);
	}
	
	@Override
	protected void _writeShort(short s, int off)
	{
		memory.putShort(this.offset + off, s);
	}
	
	@Override
	protected void _writeLong(long l, int off)
	{
		memory.putLong(this.offset + off, l);
	}
	
	@Override
	protected void _writeInt(int i)
	{
		memory.putInt(writePosi, i);
		writePosi += 4;
	}
	
	@Override
	protected void _writeShort(short s)
	{
		memory.putShort(writePosi, s);
		writePosi += 2;
	}
	
	@Override
	protected void _writeLong(long l)
	{
		memory.putLong(writePosi, l);
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
	
}
