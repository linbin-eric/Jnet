package com.jfireframework.jnet.common.buffer;

public abstract class PooledArchon implements Archon
{
	private ChunkList			cInt;
	private ChunkList			c000;
	private ChunkList			c25;
	private ChunkList			c50;
	private ChunkList			c75;
	private ChunkList			c100;
	private int					maxLevel;
	private int					unit;
	private int					maxSize;
	protected PooledIoBuffer	expansionIoBuffer;
	
	protected PooledArchon(int maxLevel, int unit)
	{
		this.maxLevel = maxLevel;
		this.unit = unit;
		// c100不具备next节点，c25不具备prev节点。
		c100 = new ChunkList(null, Integer.MAX_VALUE, 90, "c100");
		c75 = new ChunkList(c100, 100, 75, "c075");
		c50 = new ChunkList(c75, 100, 50, "c050");
		c25 = new ChunkList(c50, 75, 25, "c025");
		c000 = new ChunkList(c25, 50, 0, "c000");
		cInt = new ChunkList(c000, 25, Integer.MIN_VALUE, "cInt");
		c25.setPrev(c000);
		c50.setPrev(c25);
		c75.setPrev(c50);
		c100.setPrev(c75);
		maxSize = unit * (1 << (maxLevel));
	}
	
	public String statistics()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("cInt:").append(cInt.getStatistics().toString()).append(",").append("\r\n")//
		        .append("c000:").append(c000.getStatistics().toString()).append(",").append("\r\n")//
		        .append("c25:").append(c25.getStatistics().toString()).append(",").append("\r\n")//
		        .append("c50:").append(c50.getStatistics().toString()).append(",").append("\r\n")//
		        .append("c75:").append(c75.getStatistics().toString()).append(",").append("\r\n")//
		        .append("c100:").append(c100.getStatistics().toString());//
		return builder.toString();
	}
	
	@Override
	public synchronized void apply(int need, PooledIoBuffer handler)
	{
		if (need > maxSize)
		{
			System.out.println("申请太大");
			initHugeBuffer(handler, need);
			return;
		}
		if (c50.findChunkAndApply(need, handler, this)//
		        || c25.findChunkAndApply(need, handler, this) //
		        || c000.findChunkAndApply(need, handler, this) //
		        || cInt.findChunkAndApply(need, handler, this) //
		        || c75.findChunkAndApply(need, handler, this) //
		)
		{
			return;
		}
		Chunk chunk = newChunk(maxLevel, unit);
		chunk.archon = this;
		// 先申请，后添加
		chunk.apply(need, handler);
		cInt.addChunk(chunk);
	}
	
	@Override
	public synchronized void expansion(PooledIoBuffer buffer, int newSize)
	{
	}
	
	@Override
	public synchronized void recycle(PooledIoBuffer buffer)
	{
		if (buffer.chunk() == null)
		{
			return;
		}
		Chunk chunk = buffer.chunk();
		ChunkList list = chunk.parent();
		if (list != null)
		{
			list.recycle(buffer);
		}
		else
		{
			// 由于采用百分比计数，存在一种可能：在低于1%时，计算结果为0%。此时chunk节点已经从list删除，但是仍然被外界持有（因为实际占用率不是0）。此时的chunk节点的parent是为空。
			chunk.recycle(buffer.indexOfChunk());
		}
	}
	
	@Override
	public synchronized void recycle(PooledIoBuffer[] buffers, int off, int len)
	{
		int end = off + len;
		for (int i = off; i < end; i++)
		{
			PooledIoBuffer buffer = buffers[i];
			if (buffer.chunk() == null)
			{
				continue;
			}
			Chunk chunk = buffer.chunk();
			ChunkList list = chunk.parent();
			if (list != null)
			{
				list.recycle(buffer);
			}
			else
			{
				// 由于采用百分比计数，存在一种可能：在低于1%时，计算结果为0%。此时chunk节点已经从list删除，但是仍然被外界持有（因为实际占用率不是0）。此时的chunk节点的parent是为空。
				chunk.recycle(buffer.indexOfChunk());
			}
		}
	}
	
	/**
	 * 设置巨大内存区域的Buffer数据。该Buffer是没有关联chunk的
	 * 
	 * @param buffer
	 * @param need
	 */
	protected abstract void initHugeBuffer(PooledIoBuffer buffer, int need);
	
	protected abstract Chunk newChunk(int maxLevel, int unit);
	
	interface ExpansionIoBuffer
	{
		void clearForNextCall();
	}
	
	public static PooledArchon directPooledArchon(int maxLevel, int unit)
	{
		return new PooledDirectArchon(maxLevel, unit);
	}
	
	public static PooledArchon heapPooledArchon(int maxLevel, int unit)
	{
		return new PooledHeapArchon(maxLevel, unit);
	}
}
