package com.jfireframework.jnet.common.buffer2;

import com.jfireframework.jnet.common.util.MathUtil;

public abstract class Chunk<T>
{
	protected final int			pageSizeShift;
	protected final int			smallSizeMask;
	protected final int			maxLevel;
	protected final int[]		allocationCapacity;
	protected final int			chunkSize;
	protected int				freeBytes;
	protected T					memory;
	/* 供ChunkList使用 */
	protected ChunkList<T>	parent;
	protected Chunk<T>			next;
	protected Chunk<T>			pred;
	
	/* 供ChunkList使用 */
	
	/**
	 * 初始化一个chunk。
	 * 
	 * @param maxLevel 最大层次。起始层次为0。
	 * @param pageSize 单页字节大小。也就是一个最小的分配区域的字节数。
	 */
	public Chunk(int maxLevel, int pageSize)
	{
		this.maxLevel = maxLevel;
		pageSizeShift = MathUtil.log2(pageSize);
		smallSizeMask = ~(pageSize - 1);
		allocationCapacity = new int[1 << (maxLevel + 1)];
		for (int i = 0; i <= maxLevel; i++)
		{
			int initializeSize = calculateSize(i);
			int start = 1 << i;
			int end = (1 << (i + 1));
			for (int j = start; j < end; j++)
			{
				allocationCapacity[j] = initializeSize;
			}
		}
		freeBytes = allocationCapacity[1];
		chunkSize = freeBytes;
		memory = initializeMemory();
	}
	
	int calculateSize(int level)
	{
		return 1 << (maxLevel - level + pageSizeShift);
	}
	
	abstract T initializeMemory();
	
	/**
	 * 该chunk是否使用堆外内存
	 * 
	 * @return
	 */
	public abstract boolean isDirect();
	
	public long allocate(int normalizeSize, int reqCapacity)
	{
		if (allocationCapacity[1] < normalizeSize)
		{
			return -1;
		}
		freeBytes -= normalizeSize;
		int shift = MathUtil.log2(normalizeSize);
		int hitLevel = maxLevel - (shift - pageSizeShift);
		int index = allocateNode(normalizeSize, hitLevel);
		allocationCapacity[index] = 0;
		updateAllocatedParent(index);
		return index;
	}
	
	void initBuf(long handle, PooledBuffer<T> buffer)
	{
		int bitMap = (int) (handle >>> 32);
		if (bitMap == 0)
		{
			initNormalizeSIzeBuffer(handle, buffer);
		}
		else
		{
			
		}
	}
	
	private void initNormalizeSIzeBuffer(long handle, PooledBuffer<T> buffer)
	{
		int index = (int) handle;
		int level = MathUtil.log2((int) index);
		int capacity = calculateSize(level);
		/**
		 * 1<<hitLevel得到是该层节点数量，同时也是该层第一个节点的下标，为2的次方幂。<br/>
		 * 与index进行异或操作就可以去掉最高位的1，也就是得到了index与该值的差。
		 */
		int off = (index ^ (1 << level)) * capacity;
		buffer.init(memory, capacity, off, handle);
	}
	
	/**
	 * 回收handle处的的空间
	 * 
	 * @param handle
	 */
	public void free(long handle)
	{
		int bitMap = bitMap(handle);
		if (bitMap == 0)
		{
			freeNormal((int) handle);
		}
		else
		{
			
		}
	}
	
	int bitMap(long handle)
	{
		return (int) (handle >> 32);
	}
	
	private void freeNormal(int index)
	{
		int level = MathUtil.log2((int) index);
		int capacity = calculateSize(level);
		freeBytes += capacity;
		allocationCapacity[index] = capacity;
		while (index > 1)
		{
			int parentIndex = index >> 1;
			int value = allocationCapacity[index];
			int value2 = allocationCapacity[index ^ 1];
			int levelSize = calculateSize(level);
			if (value == levelSize && value2 == levelSize)
			{
				allocationCapacity[parentIndex] = levelSize << 1;
			}
			else
			{
				allocationCapacity[parentIndex] = value > value2 ? value : value2;
			}
			index = parentIndex;
			level -= 1;
		}
	}
	
	private int allocateNode(int normalizeSize, int hitLevel)
	{
		int childIndex = 1;
		int level = 0;
		while (level < hitLevel)
		{
			childIndex <<= 1;
			if (allocationCapacity[childIndex] < normalizeSize)
			{
				childIndex ^= 1;
			}
			level += 1;
		}
		return childIndex;
	}
	
	private void updateAllocatedParent(int index)
	{
		// 设置当前节点最大可分配为0
		while (index > 1)
		{
			int parentIndex = index >> 1;
			int value = allocationCapacity[index];
			int value2 = allocationCapacity[index ^ 1];
			int parentValue = value > value2 ? value : value2;
			allocationCapacity[parentIndex] = parentValue;
			index = parentIndex;
		}
	}
	
	public int usage()
	{
		int result = 100 - (freeBytes * 100 / chunkSize);
		if (result == 0)
		{
			return freeBytes == chunkSize ? 0 : 1;
		}
		else
		{
			return result;
		}
	}
}
