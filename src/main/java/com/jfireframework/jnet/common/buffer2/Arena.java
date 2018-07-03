package com.jfireframework.jnet.common.buffer2;

import com.jfireframework.jnet.common.util.MathUtil;

public abstract class Arena<T>
{
	ChunkList<T>			c000;
	ChunkList<T>			c025;
	ChunkList<T>			c050;
	ChunkList<T>			c075;
	ChunkList<T>			c100;
	ChunkList<T>			cInt;
	private SubPage<T>[]	tinySubPages;
	private SubPage<T>[]	smallSubPages;
	private final int		pageSize;
	private final int		pageShift;
	private final int		maxLevel;
	private final int		subpageOverflowMask;
	private final int		chunkSize;
	
	@SuppressWarnings("unchecked")
	public Arena(int maxLevel, int pageSize)
	{
		this.maxLevel = maxLevel;
		this.pageSize = pageSize;
		pageShift = MathUtil.log2(pageSize);
		subpageOverflowMask = ~(pageSize - 1);
		chunkSize = (1 << maxLevel) * pageSize;
		c100 = new ChunkList<>(100, 100, null, chunkSize);
		c075 = new ChunkList<>(75, 99, c000, chunkSize);
		c050 = new ChunkList<>(50, 99, c075, chunkSize);
		c025 = new ChunkList<>(25, 75, c050, chunkSize);
		c000 = new ChunkList<>(1, 50, c025, chunkSize);
		cInt = new ChunkList<>(0, 25, c000, chunkSize);
		c100.setPrevList(c075);
		c075.setPrevList(c050);
		c050.setPrevList(c025);
		c025.setPrevList(c000);
		// 在tiny区间，以16为大小，每一个16的倍数都占据一个槽位。为了方便定位，实际上数组的0下标是不使用的
		tinySubPages = new SubPage[512 >>> 4];
		for (int i = 0; i < tinySubPages.length; i++)
		{
			tinySubPages[i] = new SubPage<T>(pageSize);
		}
		// 在small，从1<<9开始，每一次右移都占据一个槽位，直到pagesize大小.
		smallSubPages = new SubPage[pageShift - 9];
		for (int i = 0; i < smallSubPages.length; i++)
		{
			smallSubPages[i] = new SubPage<T>(pageSize);
		}
	}
	
	abstract Chunk<T> newChunk(int maxLevel, int pageSize, int chunkSize);
	
	abstract void allocateHuge(int reqCapacity, PooledBuffer<T> buffer);
	
	abstract void destoryChunk(Chunk<T> chunk);
	
	public void allocate(int reqCapacity, int maxCapacity, PooledBuffer<T> buffer, ThreadCache cache)
	{
		int normalizeCapacity = normalizeCapacity(reqCapacity);
		if (isTinyOrSmall(normalizeCapacity))
		{
			SubPage<T> head;
			if (isTiny(normalizeCapacity))
			{
				if (cache.allocate(buffer, normalizeCapacity, SizeType.TINY))
				{
					return;
				}
				head = tinySubPages[tinyIdx(normalizeCapacity)];
			}
			else
			{
				if (cache.allocate(buffer, normalizeCapacity, SizeType.SMALL))
				{
					return;
				}
				head = smallSubPages[smallIdx(normalizeCapacity)];
			}
			synchronized (head)
			{
				SubPage<T> subPage = head.next;
				if (subPage != null)
				{
					subPage.allocate(buffer);
					return;
				}
			}
			allocateNormal(buffer, normalizeCapacity);
		}
		else if (normalizeCapacity <= chunkSize)
		{
			allocateNormal(buffer, normalizeCapacity);
		}
		else
		{
			allocateHuge(reqCapacity, buffer);
		}
	}
	
	private synchronized void allocateNormal(PooledBuffer<T> buffer, int normalizeCapacity)
	{
		if (//
		c050.allocate(normalizeCapacity, buffer)//
		        || c025.allocate(normalizeCapacity, buffer)//
		        || c000.allocate(normalizeCapacity, buffer)//
		        || cInt.allocate(normalizeCapacity, buffer)//
		        || c075.allocate(normalizeCapacity, buffer)
		
		)
		{
			return;
		}
		Chunk<T> chunk = newChunk(maxLevel, pageSize, chunkSize);
		long handle = chunk.allocate(normalizeCapacity);
		assert handle > 0;
		chunk.initBuf(handle, buffer);
		cInt.add(chunk);
	}
	
	int tinyIdx(int normalizeCapacity)
	{
		return normalizeCapacity >>> 4;
	}
	
	int smallIdx(int normalizeCapacity)
	{
		return MathUtil.log2(normalizeCapacity) - 9;
	}
	
	int normalizeCapacity(int reqCapacity)
	{
		if (reqCapacity >= chunkSize)
		{
			return reqCapacity;
		}
		if (isTiny(reqCapacity))
		{
			return (reqCapacity & 15) == 0 ? reqCapacity : (reqCapacity & ~15) + 16;
		}
		return MathUtil.tableSizeFor(reqCapacity);
	}
	
	static boolean isTiny(int normCapacity)
	{
		return (normCapacity & 0xFFFFFE00) == 0;
	}
	
	boolean isTinyOrSmall(int normCapacity)
	{
		return (normCapacity & subpageOverflowMask) == 0;
	}
	
	public void free(Chunk<T> chunk, long handle, int normalizeCapacity)
	{
		if (chunk.unpooled == true)
		{
			destoryChunk(chunk);
		}
		else
		{
			final boolean destoryChunk;
			synchronized (this)
			{
				destoryChunk = chunk.parent.free(chunk, handle);
			}
			if (destoryChunk)
			{
				destoryChunk(chunk);
			}
		}
	}
}
