package com.jfireframework.jnet.common.mem.buffer;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.handler.Handler;

public class AbstractIoBuffer<T> implements IoBuffer
{
	protected Archon<T>		archon;
	protected Handler<T>	handler;
	protected Handler<T>	expansionHandler;
	protected int			maskReadPosi;
	protected int			maskWritePosi;
	
	@Override
	public String toString()
	{
		return "AbstractIoBuffer [readPosi=" + handler.getReadPosi() + ",writePosi=" + handler.getWritePosi() + "maskReadPosi=" + maskReadPosi + ", maskWritePosi=" + maskWritePosi + "]";
	}
	
	public void initialize(Archon<T> archon, Handler<T> handler, int initSize, Handler<T> expansionHandler)
	{
		this.archon = archon;
		this.handler = handler;
		this.expansionHandler = expansionHandler;
		archon.apply(initSize, handler);
	}
	
	public Handler<T> getHandler()
	{
		return handler;
	}
	
	public Handler<T> getExpansionHandler()
	{
		return expansionHandler;
	}
	
	@Override
	public IoBuffer maskRead()
	{
		maskReadPosi = handler.getReadPosi();
		return this;
	}
	
	@Override
	public IoBuffer maskWritePosi()
	{
		maskWritePosi = handler.getWritePosi();
		return this;
	}
	
	@Override
	public IoBuffer resetReadPosi()
	{
		handler.setReadPosi(maskReadPosi);
		return this;
	}
	
	@Override
	public IoBuffer resetWritePosi()
	{
		handler.setWritePosi(maskWritePosi);
		return this;
	}
	
	@Override
	public IoBuffer readPosi(int readPosi)
	{
		handler.setReadPosi(readPosi);
		return this;
	}
	
	@Override
	public IoBuffer writePosi(int writePosi)
	{
		handler.setWritePosi(writePosi);
		return this;
	}
	
	@Override
	public int readPosi()
	{
		return handler.getReadPosi();
	}
	
	@Override
	public int writePosi()
	{
		return handler.getWritePosi();
	}
	
	@Override
	public IoBuffer put(byte b)
	{
		ensureCapacity(1);
		handler.put(b);
		return this;
	}
	
	protected void ensureCapacity(int size)
	{
		if (size <= 0)
		{
			return;
		}
		if (handler.isEnoughWrite(size) == false)
		{
			archon.apply(handler.capacity() + size, expansionHandler);
			expansionHandler.put(handler);
			archon.recycle(handler);
			Handler<T> exchange = handler;
			handler = expansionHandler;
			expansionHandler = exchange;
			expansionHandler.destory();
		}
	}
	
	@Override
	public IoBuffer put(byte b, int posi)
	{
		ensureCapacity(posi + 1 - handler.getWritePosi());
		handler.put(b, posi);
		return this;
	}
	
	@Override
	public IoBuffer put(byte[] content)
	{
		ensureCapacity(content.length);
		handler.put(content);
		return this;
	}
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		ensureCapacity(len);
		handler.put(content, off, len);
		return this;
	}
	
	@Override
	public IoBuffer clear()
	{
		handler.clear();
		return this;
	}
	
	@Override
	public byte get()
	{
		return handler.get();
	}
	
	@Override
	public byte get(int posi)
	{
		return handler.get(posi);
	}
	
	@Override
	public int remainRead()
	{
		return handler.remainRead();
	}
	
	@Override
	public int remainWrite()
	{
		return handler.remainWrite();
	}
	
	@Override
	public IoBuffer compact()
	{
		handler.compact();
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content)
	{
		handler.get(content);
		return this;
	}
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		handler.get(content, off, len);
		return this;
	}
	
	@Override
	public IoBuffer addReadPosi(int add)
	{
		handler.addReadPosi(add);
		return this;
	}
	
	@Override
	public IoBuffer addWritePosi(int add)
	{
		handler.addWritePosi(add);
		return this;
	}
	
	@Override
	public void release()
	{
		archon.recycle(handler);
		handler = null;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		return handler.indexOf(array);
	}
	
	@Override
	public int size()
	{
		return handler.capacity();
	}
	
	@Override
	public void expansion(int newSize)
	{
		ensureCapacity(newSize - size());
	}
	
	@Override
	public int readInt()
	{
		return handler.readInt();
	}
	
	@Override
	public short readShort()
	{
		return handler.readShort();
	}
	
	@Override
	public long readLong()
	{
		return handler.readLong();
	}
	
	@Override
	public IoBuffer writeInt(int i, int off)
	{
		ensureCapacity(off + 4 - handler.getWritePosi());
		handler.writeInt(i, off);
		return this;
	}
	
	@Override
	public IoBuffer writeLong(long l, int off)
	{
		ensureCapacity(off + 8 - handler.getWritePosi());
		handler.writeLong(l, off);
		return this;
	}
	
	@Override
	public IoBuffer writeShort(short s, int off)
	{
		ensureCapacity(off + 2 - handler.getWritePosi());
		handler.writeShort(s, off);
		return this;
	}
	
	@Override
	public ByteBuffer byteBuffer()
	{
		return handler.byteBuffer();
	}
	
	@Override
	public ByteBuffer cachedByteBuffer()
	{
		return handler.cachedByteBuffer();
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer)
	{
		ensureCapacity(buffer.remainRead());
		handler.put(((AbstractIoBuffer<?>) buffer).handler);
		return this;
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer, int length)
	{
		ensureCapacity(length);
		handler.put(((AbstractIoBuffer<?>) buffer).handler, length);
		return this;
	}
	
	@Override
	public IoBuffer writeInt(int i)
	{
		ensureCapacity(4);
		handler.writeInt(i);
		return this;
	}
	
	@Override
	public IoBuffer writeLong(long l)
	{
		ensureCapacity(8);
		handler.writeLong(l);
		return this;
	}
	
	@Override
	public IoBuffer writeShort(short s)
	{
		ensureCapacity(2);
		handler.writeShort(s);
		return this;
	}
}
