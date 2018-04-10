package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.ChunkList;
import com.jfireframework.jnet.common.mem.handler.AbstractIoBuffer;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public abstract class PooledArchon<T> implements Archon<T>
{
	private ChunkList<T>			cInt;
	private ChunkList<T>			c000;
	private ChunkList<T>			c25;
	private ChunkList<T>			c50;
	private ChunkList<T>			c75;
	private ChunkList<T>			c100;
	private int						maxLevel;
	private int						unit;
	private int						maxSize;
	protected ExpansionIoBuffer<T>	expansionIoBuffer;
	
	public PooledArchon(int maxLevel, int unit)
	{
		this.maxLevel = maxLevel;
		this.unit = unit;
		// c100不具备next节点，c25不具备prev节点。
		c100 = new ChunkList<T>(null, Integer.MAX_VALUE, 90, "c100");
		c75 = new ChunkList<T>(c100, 100, 75, "c075");
		c50 = new ChunkList<T>(c75, 100, 50, "c050");
		c25 = new ChunkList<T>(c50, 75, 25, "c025");
		c000 = new ChunkList<T>(c25, 50, 1, "c000");
		cInt = new ChunkList<T>(c000, 25, Integer.MIN_VALUE, "cInt");
		c25.setPrev(c000);
		c50.setPrev(c25);
		c75.setPrev(c50);
		c100.setPrev(c75);
		maxSize = unit * (1 << (maxLevel));
	}
	
	@Override
	public synchronized void apply(int need, IoBuffer<T> handler)
	{
		if (need > maxSize)
		{
			System.out.println("申请太大");
			initHugeBucket(handler, need);
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
		Chunk<T> chunk = newChunk(maxLevel, unit);
		// 先申请，后添加
		chunk.apply(need, handler, this);
		cInt.addChunk(chunk);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized void expansion(IoBuffer<T> handler, int newSize)
	{
		apply(newSize, expansionIoBuffer);
		((AbstractIoBuffer<T>) expansionIoBuffer).copy((AbstractIoBuffer<T>) handler);
		recycle(handler);
		((AbstractIoBuffer<T>) handler).replace((AbstractIoBuffer<T>) expansionIoBuffer);
		expansionIoBuffer.clearForNextCall();
	}
	
	@Override
	public synchronized void recycle(IoBuffer<T> handler)
	{
		if (handler.belong() == null)
		{
			return;
		}
		Chunk<T> chunk = handler.belong();
		ChunkList<T> list = chunk.parent();
		if (list != null)
		{
			list.recycle(handler);
		}
		else
		{
			// 由于采用百分比计数，存在一种可能：在低于1%时，计算结果为0%。此时chunk节点已经从list删除，但是仍然被外界持有（因为实际占用率不是0）。此时的chunk节点的parent是为空。
			chunk.recycle(handler);
		}
	}
	
	protected abstract void initHugeBucket(IoBuffer<T> handler, int need);
	
	protected abstract Chunk<T> newChunk(int maxLevel, int unit);
	
	interface ExpansionIoBuffer<T> extends IoBuffer<T>
	{
		void clearForNextCall();
	}
	
}
