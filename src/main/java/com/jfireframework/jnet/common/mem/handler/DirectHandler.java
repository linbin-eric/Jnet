package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

public class DirectHandler extends AbstractHandler<ByteBuffer>
{
	@Override
	public void _initialize(int off, int capacity, ByteBuffer mem, int index, Chunk<ByteBuffer> chunk, Archon<ByteBuffer> archon)
	{
		if (off != 0)
		{
			throw new IllegalArgumentException();
		}
		readPosi = writePosi = 0;
	}
	
	private void changeToWrite()
	{
		mem.limit(capacity).position(writePosi);
	}
	
	protected void changeToRead()
	{
		try
		{
			mem.limit(writePosi).position(readPosi);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("read:" + readPosi);
			System.out.println("write:" + writePosi);
		}
	}
	
	@Override
	public Handler<ByteBuffer> put(byte b)
	{
		changeToWrite();
		mem.put(b);
		writePosi += 1;
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> put(byte b, int posi)
	{
		changeToWrite();
		mem.put(posi, b);
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> put(byte[] content)
	{
		changeToWrite();
		mem.put(content);
		writePosi += content.length;
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> put(byte[] content, int off, int len)
	{
		changeToWrite();
		mem.put(content, off, len);
		writePosi += len;
		return this;
	}
	
	@Override
	public byte get()
	{
		changeToRead();
		byte b = mem.get();
		readPosi += 1;
		return b;
	}
	
	@Override
	public byte get(int posi)
	{
		changeToRead();
		byte b = mem.get(posi);
		return b;
	}
	
	@Override
	public Handler<ByteBuffer> compact()
	{
		changeToRead();
		mem.compact();
		writePosi -= readPosi;
		readPosi = 0;
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> get(byte[] content)
	{
		changeToRead();
		mem.get(content);
		readPosi += content.length;
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> get(byte[] content, int off, int len)
	{
		changeToRead();
		mem.get(content, off, len);
		readPosi += len;
		return this;
	}
	
	@Override
	public Handler<ByteBuffer> put(Handler<?> handler)
	{
		return put(handler, handler.remainRead());
	}
	
	@Override
	public Handler<ByteBuffer> put(Handler<?> handler, int len)
	{
		if (handler instanceof HeapHandler)
		{
			byte[] src = ((HeapHandler) handler).mem;
			// 需要使用直接的readPosi数据，通过方法获得都是相对数据
			changeToWrite();
			mem.put(src, ((HeapHandler) handler).readPosi, len);
			writePosi += len;
		}
		else if (handler instanceof DirectHandler)
		{
			changeToWrite();
			DirectHandler target = (DirectHandler) handler;
			target.changeToRead();
			target.mem.limit(target.mem.position() + len);
			mem.put(target.mem);
			writePosi += len;
		}
		else
		{
			throw new IllegalArgumentException();
		}
		return this;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		changeToRead();
		for (int i = readPosi; i < writePosi; i++)
		{
			boolean match = true;
			for (int l = 0; i < array.length; l++)
			{
				if (mem.get(i + l) != array[l])
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
		changeToRead();
		int result = mem.getInt();
		readPosi += 4;
		return result;
	}
	
	@Override
	public short readShort()
	{
		changeToRead();
		short s = mem.getShort();
		readPosi += 2;
		return s;
	}
	
	@Override
	public long readLong()
	{
		changeToRead();
		long l = mem.getLong();
		readPosi += 8;
		return l;
	}
	
	@Override
	public void writeInt(int i, int off)
	{
		changeToWrite();
		mem.putInt(off, i);
	}
	
	@Override
	public void writeShort(short s, int off)
	{
		changeToWrite();
		mem.putShort(off, s);
	}
	
	@Override
	public void writeLong(long l, int off)
	{
		changeToWrite();
		mem.putLong(off, l);
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		changeToRead();
		cachedByteBuffer = mem;
		return mem;
	}
	
	@Override
	public void writeInt(int i)
	{
		changeToWrite();
		mem.putInt(i);
		writePosi += 4;
	}
	
	@Override
	public void writeShort(short s)
	{
		changeToWrite();
		mem.putShort(s);
		writePosi += 2;
	}
	
	@Override
	public void writeLong(long l)
	{
		changeToWrite();
		mem.putLong(l);
		writePosi += 8;
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
	
	@Override
	public Handler<ByteBuffer> clear()
	{
		writePosi = readPosi = 0;
		return this;
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
	
	@Override
	public void copy(AbstractHandler<ByteBuffer> src)
	{
		if (src instanceof DirectHandler == false)
		{
			throw new IllegalArgumentException();
		}
		DirectHandler copySrc = (DirectHandler) src;
		ByteBuffer srcBuffer = copySrc.mem;
		int srcWritePosi = copySrc.writePosi;
		int srcReadPosi = copySrc.readPosi;
		srcBuffer.position(0).limit(srcWritePosi);
		if (writePosi != 0)
		{
			throw new IllegalArgumentException();
		}
		mem.position(0).limit(capacity);
		mem.put(srcBuffer);
		readPosi = srcReadPosi;
		writePosi = srcWritePosi;
	}
	
	@Override
	public void replace(AbstractHandler<ByteBuffer> src)
	{
		if (src instanceof DirectHandler == false)
		{
			throw new IllegalArgumentException();
		}
		DirectHandler replaceSrc = (DirectHandler) src;
		mem = replaceSrc.mem;
		chunk = replaceSrc.chunk;
		readPosi = replaceSrc.readPosi;
		writePosi = replaceSrc.writePosi;
		capacity = replaceSrc.capacity;
		index = replaceSrc.index;
	}
	
}
