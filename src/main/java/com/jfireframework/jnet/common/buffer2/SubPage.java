package com.jfireframework.jnet.common.buffer2;

public class SubPage<T>
{
	int				elementSize;
	long[]			bitMap;
	int				bitMapLength;
	final Chunk<T>	chunk;
	final int		pageSize;
	final int		index;
	final int		offset;
	SubPage<T>		prev;
	SubPage<T>		next;
	
	/**
	 * 这是一个特殊节点，不参与分配，仅用做标识
	 * 
	 * @param pageSize
	 */
	public SubPage(int pageSize)
	{
		chunk = null;
		index = 0;
		offset = 0;
		this.pageSize = pageSize;
		prev = next = this;
	}
	
	public SubPage(Chunk<T> chunk, int pageSize, int index, int offset, int elementSize)
	{
		this.chunk = chunk;
		this.index = index;
		this.offset = offset;
		this.pageSize = pageSize;
		this.elementSize = elementSize;
		// elementSize最小是16。一个long可以表达64个元素
		bitMap = new long[pageSize >> 4 >> 6];
	}
	
	public void init(int elementSize)
	{
		
	}
}
