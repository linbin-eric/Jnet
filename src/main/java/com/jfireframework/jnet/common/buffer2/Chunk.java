package com.jfireframework.jnet.common.buffer2;

import com.jfireframework.jnet.common.buffer.Archon;

abstract class ChunkMetaData
{
	protected final int		pageSizeShift;
	protected final int		maxLevel;
	protected final int[]	allocationCapacity;
	protected final int		chunkSize;
	protected int			freeBytes;
	
	/**
	 * 初始化一个chunk。
	 * 
	 * @param maxLevel 最大层次。起始层次为0。
	 * @param pageSize 单页字节大小。也就是一个最小的分配区域的字节数。
	 */
	ChunkMetaData(int maxLevel, int pageSize)
	{
		this.maxLevel = maxLevel;
		pageSizeShift = Archon.log2(pageSize);
		allocationCapacity = new int[1 << (maxLevel + 1)];
		for (int i = 0; i <= maxLevel; i++)
		{
			int initializeSize = initializeSize(i);
			int start = 1 << i;
			int end = (1 << (i + 1));
			for (int j = start; j < end; j++)
			{
				allocationCapacity[j] = initializeSize;
			}
		}
		freeBytes = allocationCapacity[1];
		chunkSize = freeBytes;
	}
	
	int initializeSize(int level)
	{
		return 1 << (maxLevel - level + pageSizeShift);
	}
}

abstract class ForChunkListInfo<T> extends ChunkMetaData
{
	protected Chunk<T>	next;
	protected Chunk<T>	pred;
	
	ForChunkListInfo(int maxLevel, int pageSize)
	{
		super(maxLevel, pageSize);
	}
	
}

public abstract class Chunk<T> extends ForChunkListInfo<T>
{
	protected T memory;
	
	Chunk(int maxLevel, int pageSize)
	{
		super(maxLevel, pageSize);
		memory = initializeMemory();
	}
	
	abstract T initializeMemory();
	
	/**
	 * 该chunk是否使用堆外内存
	 * 
	 * @return
	 */
	public abstract boolean isDirect();
}
