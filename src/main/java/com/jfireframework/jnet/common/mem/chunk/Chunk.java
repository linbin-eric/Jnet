package com.jfireframework.jnet.common.mem.chunk;

import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.handler.Handler;

public abstract class Chunk<T>
{
	protected int[]			pool;
	protected T				mem;
	protected int[]			size;
	protected int			capacity;
	protected Chunk<T>		pred;
	protected Chunk<T>		next;
	protected ChunkList<T>	parent;
	protected int			pageShift;
	protected int			maxLevel;
	protected int			unit;
	
	public Chunk(int maxLevel, int unit)
	{
		this.unit = unit;
		this.maxLevel = maxLevel;
		pageShift = log2(unit);
		pool = new int[1 << (maxLevel + 1)];
		for (int i = 0; i <= maxLevel; i++)
		{
			int start = 1 << i;
			int end = (1 << (i + 1)) - 1;
			for (int j = start; j <= end; j++)
			{
				pool[j] = unit * (1 << (maxLevel - i));
			}
		}
		size = new int[maxLevel + 1];
		for (int i = 0; i <= maxLevel; i++)
		{
			size[i] = unit * (1 << (maxLevel - i));
		}
		capacity = size[0];
		mem = initializeMem(capacity);
	}
	
	static final int tableSizeFor(int cap)
	{
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return n + 1;
	}
	
	static int log2(int value)
	{
		return 31 - Integer.numberOfLeadingZeros(value);
	}
	
	protected abstract T initializeMem(int capacity);
	
	public boolean apply(int need, Handler<T> bucket, Archon<T> archon)
	{
		if (pool[1] < need)
		{
			return false;
		}
		if (need > capacity)
		{
			return false;
		}
		int selectedLevel;
		if (need > unit)
		{
			selectedLevel = maxLevel - (log2(tableSizeFor(need)) - pageShift);
		}
		else
		{
			selectedLevel = maxLevel;
		}
		int start = 1 << selectedLevel;
		int end = (1 << (selectedLevel + 1)) - 1;
		int bucketSize = size[selectedLevel];
		for (int i = start; i <= end; i++)
		{
			if (pool[i] == bucketSize && isAvailable(i, bucketSize))
			{
				reduce(i, bucketSize);
				int off = (i - (1 << selectedLevel)) * bucketSize;
				initHandler(archon, bucket, i, off, bucketSize);
				return true;
			}
		}
		return false;
	}
	
	protected abstract void initHandler(Archon<T> archon, Handler<T> bucket, int index, int off, int len);
	
	public void recycle(Handler<T> bucket)
	{
		int index = bucket.getIndex();
		int recycle = bucket.capacity();
		while (index > 0)
		{
			pool[index] += recycle;
			index >>= 1;
		}
	}
	
	private boolean isAvailable(int index, int reduce)
	{
		while (index > 0)
		{
			if (pool[index] >= reduce)
			{
				index >>= 1;
			}
			else
			{
				break;
			}
		}
		return index == 0;
	}
	
	private void reduce(int index, int reduce)
	{
		while (index > 0)
		{
			pool[index] -= reduce;
			index >>= 1;
		}
	}
	
	public int usage()
	{
		return (capacity - pool[1]) * 100 / capacity;
	}
	
	public int capacity()
	{
		return capacity;
	}
	
	public ChunkList<T> parent()
	{
		return parent;
	}
}
