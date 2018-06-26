package com.jfireframework.jnet.common.buffer2;

public class PoolChunkList<T>
{
	final int			maxUsage;
	final int			minUsage;
	final int			maxCapacity;
	PoolChunkList<T>	prev;
	PoolChunkList<T>	next;
	Chunk<T>			head;
	
	public PoolChunkList(int maxUsage, int minUsage, PoolChunkList<T> next, int chunkSize)
	{
		this.maxUsage = maxUsage;
		this.minUsage = minUsage;
		maxCapacity = calcuteMaxCapacity(minUsage, chunkSize);
		this.next = next;
	}
	
	int calcuteMaxCapacity(int minUsage, int chunkSize)
	{
		if (minUsage < 0)
		{
			return chunkSize;
		}
		else if (minUsage > 100)
		{
			return 0;
		}
		else
		{
			return (100 - minUsage) * chunkSize;
			
		}
	}
	
	public boolean allocate(int normalizeSize, int reqCapacity, PooledBuffer<T> buffer)
	{
		if (head == null || normalizeSize > maxCapacity)
		{
			return false;
		}
		Chunk<T> cursor = head;
		long handle = cursor.allocate(normalizeSize, reqCapacity);
		if (handle != -1)
		{
			cursor.initBuf(handle, buffer);
			int usage = cursor.usage();
			if (usage >= maxUsage)
			{
				remove(cursor);
				next.addFromPrev(cursor, usage);
			}
			return true;
		}
		while (cursor.next != null)
		{
			cursor = cursor.next;
			handle = cursor.allocate(normalizeSize, reqCapacity);
			if (handle != -1)
			{
				cursor.initBuf(handle, buffer);
				return true;
			}
		}
		return false;
	}
	
	public void free(Chunk<T> chunk, long handle)
	{
		chunk.free(handle);
		int usage = chunk.usage();
		if (usage <= minUsage)
		{
			
		}
	}
	
	void remove(Chunk<T> node)
	{
		Chunk<T> head = this.head;
		if (node == head)
		{
			head = node.next;
			if (head != null)
			{
				head.pred = null;
			}
			this.head = head;
		}
		else
		{
			Chunk<T> next = node.next;
			node.pred.next = next;
			if (next != null)
			{
				next.pred = node.pred;
			}
		}
	}
	
	void addFromNext(Chunk<T> chunk, int usage)
	{
		if (usage <= minUsage)
		{
			prev.addFromNext(chunk, usage);
			return;
		}
		add(chunk);
	}
	
	void add(Chunk<T> chunk)
	{
		chunk.parent = this;
		if (head == null)
		{
			head = chunk;
			chunk.pred = null;
			chunk.next = null;
		}
		else
		{
			chunk.pred = null;
			chunk.next = head;
			head.pred = chunk;
			head = chunk;
		}
	}
	
	void addFromPrev(Chunk<T> chunk, int usage)
	{
		if (usage >= maxUsage)
		{
			next.addFromPrev(chunk, usage);
			return;
		}
		add(chunk);
	}
}
