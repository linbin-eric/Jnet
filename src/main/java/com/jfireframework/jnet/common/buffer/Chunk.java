package com.jfireframework.jnet.common.buffer;

public abstract class Chunk
{
	protected int[]		pool;
	protected int		capacity;
	protected Chunk		pred;
	protected Chunk		next;
	protected ChunkList	parent;
	protected int		pageShift;
	protected int		maxLevel;
	protected int		unit;
	protected final int	capacityShift;
	protected int		freeCapaticy;
	
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
		capacity = pool[1];
		freeCapaticy = capacity;
		initializeMem(capacity);
		capacityShift = log2(capacity);
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
		need = tableSizeFor(need);
		if (pool[1] < need)
		{
			return false;
		}
		freeCapaticy -= need;
		int selectedLevel = maxLevel - (log2(need) - pageShift);
		// if (need > unit)
		// {
		// selectedLevel = maxLevel - (log2(need) - pageShift);
		// }
		// else
		// {
		// selectedLevel = maxLevel;
		// }
		int index = findAvailable(need, selectedLevel);
		reduce(index, need);
		int off = (index - (1 << selectedLevel)) * need;
		initHandler(archon, buffer, index, off, need);
		return true;
	}
	
	protected abstract void initHandler(Archon archon, IoBuffer bucket, int index, int off, int len);
	
	public void recycle(IoBuffer bucket)
	{
		int index = bucket.getIndex();
		int recycle = bucket.capacity();
		freeCapaticy += recycle;
		pool[index] = recycle;
		while (index > 1)
		{
			int parentIndex = index >> 1;
			int value = pool[index];
			int value2 = pool[index ^ 1];
			int maxCapacity = unit * (1 << (maxLevel - log2(index)));
			if (value == maxCapacity && value2 == maxCapacity)
			{
				pool[parentIndex] = maxCapacity << 1;
			}
			else
			{
				pool[parentIndex] = value > value2 ? value : value2;
			}
			index = parentIndex;
		}
	}
	
	public static void main(String[] args)
	{
		System.out.println(log2(7));
	}
	
	private int findAvailable(int reduce, int selectedLevel)
	{
		int index = 1;
		int level = 0;
		while (level < selectedLevel)
		{
			index <<= 1;
			if (pool[index] < reduce)
			{
				index ^= 1;
			}
			level += 1;
		}
		return index;
		// if (pool[index] == reduce)
		// {
		// return index;
		// }
		// else
		// {
		// return index ^ 1;
		// }
	}
	
	private void reduce(int index, int reduce)
	{
		// 设置当前节点最大可分配为0
		pool[index] = 0;
		while (index > 1)
		{
			int parentIndex = index >> 1;
			int value = pool[index];
			int value2 = pool[index ^ 1];
			int parentValue = value > value2 ? value : value2;
			pool[parentIndex] = parentValue;
			index = parentIndex;
		}
	}
	
	/**
	 * 针对0%做一个特殊处理。除非所有耗尽，否则最低显示1%
	 * 
	 * @return
	 */
	public int usage()
	{
		int left = capacity - freeCapaticy;
		int result = (left * 100) >> capacityShift;
		if (result == 0)
		{
			if (left != 0)
			{
				return 1;
			}
			else
			{
				return result;
			}
		}
		else
		{
			return result;
		}
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
