package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.Recycler.RecycleHandler;

public abstract class PooledBuffer<T> implements IoBuffer
{
	T				memory;
	int				capacity;
	int				readPosi;
	int				writePosi;
	// 可用位置相对于memory的偏移量
	int				offset;
	long			handle;
	ThreadCache		cache;
	Chunk<T>		chunk;
	RecycleHandler	recycleHandler;
	
	public void init(Chunk<T> chunk, int capacity, int offset, long handle, ThreadCache cache)
	{
		this.chunk = chunk;
		memory = chunk.memory;
		this.offset = offset;
		this.capacity = capacity;
		this.handle = handle;
		this.cache = cache;
		readPosi = writePosi = 0;
	}
	
	public void initUnPooled(Chunk<T> chunk, ThreadCache cache)
	{
		this.chunk = chunk;
		this.cache = cache;
		memory = chunk.memory;
		offset = 0;
		readPosi = writePosi = 0;
		capacity = chunk.chunkSize;
		handle = -1;
	}
	
	@Override
	public int capacity()
	{
		return capacity;
	}
	
	int nextWritePosi(int length)
	{
		int oldPosi = writePosi;
		int posi = oldPosi + length;
		if (posi > capacity)
		{
			reAllocate(posi);
		}
		writePosi = posi;
		return oldPosi;
	}
	
	void reAllocate(int reqCapacity)
	{
		chunk.arena.reAllocate(this, reqCapacity);
	}
	
	@Override
	public IoBuffer put(byte b)
	{
		int posi = nextWritePosi(1);
		put0(posi, b);
		return this;
	}
	
	abstract void put0(int posi, byte value);
	
	void checkWritePosi(int posi, int length)
	{
		int newPosi = posi + length;
		if (newPosi > capacity)
		{
			reAllocate(newPosi);
		}
	}
	
	@Override
	public IoBuffer put(byte b, int posi)
	{
		checkWritePosi(posi, 1);
		put0(posi, b);
		return this;
	}
	
	@Override
	public IoBuffer put(byte[] content)
	{
		int length = content.length;
		int posi = nextWritePosi(length);
		put0(content, 0, length, posi);
		return this;
	}
	
	abstract void put0(byte[] content, int off, int len, int posi);
	
	@Override
	public IoBuffer put(byte[] content, int off, int len)
	{
		int posi = nextWritePosi(len);
		put0(content, off, len, posi);
		return this;
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer)
	{
		return put(buffer, buffer.remainRead());
	}
	
	@Override
	public IoBuffer put(IoBuffer buffer, int len)
	{
		if (buffer.remainRead() < len)
		{
			throw new IllegalArgumentException("剩余读取长度不足");
		}
		if (buffer instanceof PooledHeapBuffer)
		{
			put1((PooledHeapBuffer) buffer, len);
		}
		else if (buffer instanceof PooledDirectBuffer)
		{
			put2((PooledDirectBuffer) buffer, len);
		}
		else
		{
			int originReadPosi = buffer.getReadPosi();
			for (int i = 0; i < len; i++)
			{
				put(buffer.get());
			}
			buffer.setReadPosi(originReadPosi);
		}
		return this;
	}
	
	abstract void put1(PooledHeapBuffer buffer, int len);
	
	abstract void put2(PooledDirectBuffer buffer, int len);
	
	@Override
	public IoBuffer putInt(int i)
	{
		int posi = nextWritePosi(4);
		putInt0(posi, i);
		return this;
	}
	
	abstract void putInt0(int posi, int value);
	
	@Override
	public IoBuffer putInt(int value, int posi)
	{
		checkWritePosi(posi, 4);
		putInt0(posi, value);
		return this;
	}
	
	@Override
	public IoBuffer putShort(short value, int posi)
	{
		checkWritePosi(posi, 2);
		putShort0(posi, value);
		return this;
	}
	
	abstract void putShort0(int posi, short value);
	
	@Override
	public IoBuffer putLong(long value, int posi)
	{
		checkWritePosi(posi, 8);
		putLong0(posi, value);
		return this;
	}
	
	abstract void putLong0(int posi, long value);
	
	@Override
	public IoBuffer putShort(short s)
	{
		int posi = nextWritePosi(2);
		putShort0(posi, s);
		return this;
	}
	
	@Override
	public IoBuffer putLong(long l)
	{
		int posi = nextWritePosi(8);
		putLong0(posi, l);
		return this;
	}
	
	@Override
	public int getReadPosi()
	{
		return readPosi;
	}
	
	@Override
	public IoBuffer setReadPosi(int readPosi)
	{
		this.readPosi = readPosi;
		return this;
	}
	
	@Override
	public int getWritePosi()
	{
		return writePosi;
	}
	
	@Override
	public IoBuffer setWritePosi(int writePosi)
	{
		this.writePosi = writePosi;
		return this;
	}
	
	@Override
	public IoBuffer clear()
	{
		readPosi = writePosi = 0;
		return this;
	}
	
	@Override
	public IoBuffer clearAndErasureData()
	{
		for (int i = 0; i < capacity; i++)
		{
			put0(i, (byte) 0);
		}
		readPosi = writePosi = 0;
		return this;
	}
	
	int nextReadPosi(int len)
	{
		int oldPosi = readPosi;
		int newPosi = oldPosi + len;
		if (newPosi > writePosi)
		{
			throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
		}
		readPosi = newPosi;
		return oldPosi;
	}
	
	@Override
	public byte get()
	{
		int posi = nextReadPosi(1);
		return get0(posi);
	}
	
	abstract byte get0(int posi);
	
	void checkReadPosi(int posi, int len)
	{
		posi += len;
		if (posi > writePosi)
		{
			throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
		}
	}
	
	@Override
	public byte get(int posi)
	{
		checkReadPosi(posi, 1);
		return get0(posi);
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
	public IoBuffer get(byte[] content)
	{
		return get(content, 0, content.length);
	}
	
	abstract void get0(byte[] content, int off, int len, int posi);
	
	@Override
	public IoBuffer get(byte[] content, int off, int len)
	{
		int posi = nextReadPosi(len);
		get0(content, off, len, posi);
		return this;
	}
	
	@Override
	public IoBuffer addReadPosi(int add)
	{
		readPosi += add;
		return this;
	}
	
	@Override
	public IoBuffer addWritePosi(int add)
	{
		writePosi += add;
		if (writePosi > capacity)
		{
			reAllocate(writePosi);
		}
		return this;
	}
	
	@Override
	public IoBuffer capacityReadyFor(int newCapacity)
	{
		if (newCapacity <= capacity)
		{
			;
		}
		else
		{
			reAllocate(newCapacity);
		}
		return this;
	}
	
	@Override
	public int indexOf(byte[] array)
	{
		for (int i = readPosi; i < writePosi; i++)
		{
			if (get(i) == array[0])
			{
				int length = array.length;
				if (writePosi - i < length)
				{
					return -1;
				}
				boolean miss = false;
				for (int l = 0; l < length; l++)
				{
					if (get(i + l) != array[l])
					{
						miss = true;
						break;
					}
				}
				if (miss == false)
				{
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public int getInt()
	{
		int posi = nextReadPosi(4);
		return getInt0(posi);
	}
	
	abstract int getInt0(int posi);
	
	@Override
	public short getShort()
	{
		int posi = nextReadPosi(2);
		return getShort0(posi);
	}
	
	abstract short getShort0(int posi);
	
	@Override
	public long getLong()
	{
		int posi = nextReadPosi(8);
		return getLong0(posi);
	}
	
	@Override
	public int getInt(int posi)
	{
		checkReadPosi(posi, 4);
		return getInt0(posi);
	}
	
	@Override
	public short getShort(int posi)
	{
		checkReadPosi(posi, 2);
		return getShort0(posi);
	}
	
	@Override
	public long getLong(int posi)
	{
		checkReadPosi(posi, 4);
		return getLong0(posi);
	}
	
	abstract long getLong0(int posi);
	
	@Override
	public void free()
	{
		chunk.arena.free(chunk, handle, capacity, cache);
		handle = offset = capacity = readPosi = writePosi = 0;
		memory = null;
		cache = null;
		chunk = null;
		if (recycleHandler != null)
		{
			RecycleHandler tmp = recycleHandler;
			recycleHandler = null;
			tmp.recycle(this);
		}
	}
	
}
