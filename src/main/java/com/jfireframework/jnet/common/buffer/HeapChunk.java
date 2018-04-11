package com.jfireframework.jnet.common.buffer;

class HeapChunk extends Chunk
{
	private byte[] mem;
	
	public HeapChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initializeMem(int capacity)
	{
		mem = new byte[capacity];
	}
	
	@Override
	protected void initHandler(Archon archon, IoBuffer handler, int index, int off, int len)
	{
		handler.initialize(off, len, mem, index, this, archon);
	}
	
}
