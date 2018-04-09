package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

public class HeapHandler extends AbstractHandler<byte[]>
{
	
	private int offset;
	
	@Override
	public String toString()
	{
		return "HeapHandler [readPosi=" + readPosi + ", writePosi=" + writePosi + ", capacity=" + capacity + ", offset=" + offset + "]";
	}
	
	@Override
	public void _initialize(int off, int len, byte[] mem, int index, Chunk<byte[]> chunk, Archon<byte[]> archon)
	{
		this.offset = off;
		readPosi = writePosi = off;
	}
	
	@Override
	public Handler<byte[]> put(byte b)
	{
		mem[writePosi] = b;
		writePosi += 1;
		return this;
	}
	
	@Override
	public Handler<byte[]> put(byte b, int posi)
	{
		mem[offset + posi] = b;
		return this;
	}
	
	@Override
	public Handler<byte[]> put(byte[] content)
	{
		System.arraycopy(content, 0, mem, writePosi, content.length);
		writePosi += content.length;
		return this;
	}
	
	@Override
	public Handler<byte[]> put(byte[] content, int off, int len)
	{
		System.arraycopy(content, off, mem, writePosi, len);
		writePosi += len;
		return this;
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
	public Handler<byte[]> compact()
	{
		int length = remainRead();
		System.arraycopy(mem, readPosi, mem, offset, length);
		readPosi = offset;
		writePosi -= length;
		return this;
	}
	
	@Override
	public Handler<byte[]> get(byte[] content)
	{
		System.arraycopy(mem, readPosi, content, 0, content.length);
		readPosi += content.length;
		return this;
	}
	
	@Override
	public Handler<byte[]> get(byte[] content, int off, int len)
	{
		System.arraycopy(mem, readPosi, content, off, len);
		readPosi += len;
		return this;
	}
	
	@Override
	public Handler<byte[]> put(Handler<?> handler)
	{
		return put(handler, handler.remainRead());
	}
	
	@Override
	public Handler<byte[]> put(Handler<?> handler, int len)
	{
		if (handler instanceof HeapHandler)
		{
			HeapHandler target = (HeapHandler) handler;
			System.arraycopy(target.mem, target.readPosi, mem, writePosi, len);
			writePosi += len;
			return this;
		}
		else if (handler instanceof DirectHandler)
		{
			DirectHandler source = (DirectHandler) handler;
			source.changeToRead();
			source.mem.get(mem, writePosi, len);
			writePosi += len;
			return this;
		}
		else
		{
			throw new UnsupportedOperationException();
		}
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
	public void writeInt(int i, int off)
	{
		off += this.offset;
		mem[off] = (byte) (i >> 24);
		mem[off + 1] = (byte) (i >> 16);
		mem[off + 2] = (byte) (i >> 8);
		mem[off + 3] = (byte) (i);
	}
	
	@Override
	public void writeShort(short s, int off)
	{
		off += this.offset;
		mem[off] = (byte) (s >> 8);
		mem[off + 1] = (byte) (s);
	}
	
	@Override
	public void writeLong(long l, int off)
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
	public ByteBuffer byteBuffer()
	{
		cachedByteBuffer = ByteBuffer.wrap(mem, readPosi, remainRead());
		return cachedByteBuffer;
	}
	
	@Override
	public void writeInt(int i)
	{
		mem[writePosi] = (byte) (i >> 24);
		mem[writePosi + 1] = (byte) (i >> 16);
		mem[writePosi + 2] = (byte) (i >> 8);
		mem[writePosi + 3] = (byte) (i);
		writePosi += 4;
	}
	
	@Override
	public void writeShort(short s)
	{
		mem[writePosi] = (byte) (s >> 8);
		mem[writePosi + 1] = (byte) (s);
		writePosi += 2;
	}
	
	@Override
	public void writeLong(long l)
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
	public int capacity()
	{
		return capacity;
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
	public Handler<byte[]> clear()
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
	
}
