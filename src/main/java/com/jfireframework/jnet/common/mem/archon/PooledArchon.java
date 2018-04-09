package com.jfireframework.jnet.common.mem.archon;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.ChunkList;
import com.jfireframework.jnet.common.mem.handler.Handler;

public abstract class PooledArchon<T> implements Archon<T>
{
	private ChunkList<T>		cInt;
	private ChunkList<T>		c000;
	private ChunkList<T>		c25;
	private ChunkList<T>		c50;
	private ChunkList<T>		c75;
	private ChunkList<T>		c100;
	private int					maxLevel;
	private int					unit;
	private int					maxSize;
	private ExpansionHandler	expansionHandler	= new ExpansionHandler();
	
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
	public synchronized void apply(int need, Handler<T> handler)
	{
		if (need > maxSize)
		{
			System.out.println("申请太大");
			initHugeBucket(handler, need);
			return;
		}
		if (c50.findChunkAndApply(need, handler)//
		        || c25.findChunkAndApply(need, handler) //
		        || c000.findChunkAndApply(need, handler) //
		        || cInt.findChunkAndApply(need, handler) //
		        || c75.findChunkAndApply(need, handler) //
		)
		{
			return;
		}
		Chunk<T> chunk = newChunk(maxLevel, unit);
		// 先申请，后添加
		chunk.apply(need, handler);
		cInt.addChunk(chunk);
	}
	
	@Override
	public synchronized void expansion(Handler<T> handler, int newSize)
	{
		apply(newSize, expansionHandler);
		expansionHandler.put(handler);
		recycle(handler);
		
		expansionHandler.clearForNext();
		
	}
	
	@Override
	public synchronized void recycle(Handler<T> handler)
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
	
	protected abstract void initHugeBucket(Handler<T> handler, int need);
	
	protected abstract Chunk<T> newChunk(int maxLevel, int unit);
	
	class ExpansionHandler implements Handler<T>
	{
		private int			index;
		private Chunk<T>	chunk;
		private int			off;
		private int			len;
		private T			mem;
		
		public void clearForNext()
		{
			mem = null;
			chunk = null;
		}
		
		@Override
		public void initialize(int off, int len, T mem, int index, Chunk<T> chunk, Archon<T> archon)
		{
			this.off = off;
			this.len = len;
			this.mem = mem;
			this.index = index;
			this.chunk = chunk;
		}
		
		@Override
		public void destory()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Chunk<T> belong()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getIndex()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int capacity()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getReadPosi()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setReadPosi(int readPosi)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getWritePosi()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void setWritePosi(int writePosi)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(byte b)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(byte b, int posi)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(byte[] content)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(byte[] content, int off, int len)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> clear()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public byte get()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public byte get(int posi)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int remainRead()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int remainWrite()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> compact()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> get(byte[] content)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> get(byte[] content, int off, int len)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(Handler<?> bucket)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Handler<T> put(Handler<?> bucket, int len)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isEnoughWrite(int size)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addReadPosi(int add)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void addWritePosi(int add)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int indexOf(byte[] array)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int readInt()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public short readShort()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public long readLong()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeInt(int i, int off)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeShort(short s, int off)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeLong(long l, int off)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeInt(int i)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeShort(short s)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void writeLong(long l)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ByteBuffer byteBuffer()
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ByteBuffer cachedByteBuffer()
		{
			throw new UnsupportedOperationException();
		}
		
	}
}
