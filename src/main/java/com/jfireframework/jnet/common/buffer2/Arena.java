package com.jfireframework.jnet.common.buffer2;

import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.SystemPropertyUtil;

public abstract class Arena<T>
{
	public static final int	PAGESIZE;
	public static final int	MAXLEVEL;
	public static final int	CHUNKSIZE;
	static
	{
		int maxLevel = SystemPropertyUtil.getInt("io.jnet.arena.maxLevel", 11);
		MAXLEVEL = Math.min(maxLevel, 30);
		int pageSize = SystemPropertyUtil.getInt("io.jnet.arena.pageSize", 8 * 1024);
		PAGESIZE = Math.max(pageSize, 4 * 1024);
		CHUNKSIZE = (1 << MAXLEVEL) * PAGESIZE;
	}
	ChunkList<T>		c000;
	ChunkList<T>		c025;
	ChunkList<T>		c050;
	ChunkList<T>		c075;
	ChunkList<T>		c100;
	ChunkList<T>		cInt;
	private final int	pageSize;
	private final int	maxLevel;
	private final int	subpageOverflowMask;
	private final int	chunkSize;
	
	public Arena(int maxLevel, int pageSize)
	{
		this.maxLevel = maxLevel;
		this.pageSize = pageSize;
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
	}
	
	public void allocate(int reqCapacity, int maxCapacity, PooledBuffer<T> buffer)
	{
		int normalizeCapacity = normalizeCapacity(reqCapacity);
		if (isTinyOrSmall(normalizeCapacity))
		{
			
		}
		else if (normalizeCapacity <= chunkSize)
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
		else
		{
			allocateHuge(reqCapacity, buffer);
		}
	}
	
	abstract Chunk<T> newChunk(int maxLevel, int pageSize, int chunkSize);
	
	abstract void allocateHuge(int reqCapacity, PooledBuffer<T> buffer);
	
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
}
