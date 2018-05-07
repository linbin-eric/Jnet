package com.jfireframework.jnet.common.buffer;

public abstract class Chunk
{
	/**
	 * 如果是Heap类型的Chunk则不为空
	 */
	protected byte[]	array;
	/**
	 * 如果是Direct类型的Chunk则不为-1
	 */
	protected long		address;
	protected Chunk		pred;
	protected Chunk		next;
	protected ChunkList	parent;
	protected Archon	archon;
	protected final int	pageShift;
	protected final int	maxLevel;
	protected final int	pageSize;
	protected int[]		pool;
	// 当前chunk的最大容量
	protected final int	capacity;
	// 剩余可分配字节
	protected int		freeCapaticy;
	protected final int	capacityShift;
	
	/**
	 * 初始化一个chunk。
	 * 
	 * @param maxLevel 最大层次。起始层次为0。
	 * @param pageSize 单页字节大小。也就是一个最小的分配区域的字节数。
	 */
	public Chunk(int maxLevel, int pageSize)
	{
		this.pageSize = pageSize;
		this.maxLevel = maxLevel;
		pageShift = log2(pageSize);
		pool = new int[1 << (maxLevel + 1)];
		for (int i = 0; i <= maxLevel; i++)
		{
			int start = 1 << i;
			int end = (1 << (i + 1)) - 1;
			for (int j = start; j <= end; j++)
			{
				pool[j] = (1 << (maxLevel - i + pageShift));
			}
		}
		capacity = pool[1];
		freeCapaticy = capacity;
		capacityShift = log2(capacity);
		initializeMem(capacity);
	}
	
	protected abstract void initializeMem(int capacity);
	
	/**
	 * 该chunk是否使用堆外内存
	 * 
	 * @return
	 */
	public abstract boolean isDirect();
	
	/**
	 * 申请一块不小于need的内存区域。用于初始化或者扩容buffer
	 * 
	 * @param need
	 * @param buffer
	 * @param expansion 是否扩容操作
	 * @return
	 */
	public boolean apply(int need, PooledIoBuffer buffer, boolean expansion)
	{
		int capacity = need < pageSize ? pageSize : tableSizeFor(need);
		int capacityShift = log2(capacity);
		if (pool[1] < capacity)
		{
			return false;
		}
		freeCapaticy -= capacity;
		int selectedLevel = maxLevel - (capacityShift - pageShift);
		int index = findAvailable(capacity, selectedLevel);
		reduce(index, capacity);
		int off = (index - (1 << selectedLevel)) << capacityShift;
		if (expansion)
		{
			expansionBuffer(buffer, index, off, capacity);
		}
		else
		{
			initBuffer(buffer, index, off, capacity);
		}
		return true;
	}
	
	/**
	 * 使用内存区域执行初始化buffer操作
	 * 
	 * @param buffer
	 * @param index
	 * @param off
	 * @param capacity
	 */
	protected abstract void initBuffer(PooledIoBuffer buffer, int index, int off, int capacity);
	
	/**
	 * 使用内存区域执行扩容Buffer操作
	 * 
	 * @param buffer
	 * @param index
	 * @param off
	 * @param capacity
	 */
	protected abstract void expansionBuffer(PooledIoBuffer buffer, int index, int off, int capacity);
	
	/**
	 * 坐标index位置的内存区域可以重新使用
	 * 
	 * @param index
	 */
	public void recycle(int index)
	{
		int recycle = 1 << (maxLevel - log2(index) + pageShift);
		freeCapaticy += recycle;
		pool[index] = recycle;
		while (index > 1)
		{
			int parentIndex = index >> 1;
			int value = pool[index];
			int value2 = pool[index ^ 1];
			int maxCapacity = 1 << (maxLevel - log2(index) + pageShift);
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
	
}
