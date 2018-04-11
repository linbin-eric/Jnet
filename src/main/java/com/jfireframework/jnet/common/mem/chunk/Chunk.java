package com.jfireframework.jnet.common.mem.chunk;

import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public abstract class Chunk
{
	protected int[]		pool;
	protected int[]		size;
	protected int		capacity;
	protected Chunk		pred;
	protected Chunk		next;
	protected ChunkList	parent;
	protected int		pageShift;
	protected int		maxLevel;
	protected int		unit;
	
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
		initializeMem(capacity);
	}
	
	public static Chunk newHeapChunk(int maxLevel, int unit)
	{
		return new HeapChunk(maxLevel, unit);
	}
	
	public static Chunk newDirectChunk(int maxLevel, int unit)
	{
		return new DirectChunk(maxLevel, unit);
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
	
	protected abstract void initializeMem(int capacity);
	
	public boolean apply(int need, IoBuffer buffer, Archon archon)
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
				initHandler(archon, buffer, i, off, bucketSize);
				return true;
			}
		}
		return false;
	}
	
	protected abstract void initHandler(Archon archon, IoBuffer bucket, int index, int off, int len);
	
	public void recycle(IoBuffer bucket)
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
	
	public ChunkList parent()
	{
		return parent;
	}
	
}
