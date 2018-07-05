package com.jfireframework.jnet.common.buffer;

public class HeapArena extends Arena<byte[]>
{
	
	public HeapArena(PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
	{
		super(parent, maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
	}
	
	@Override
	void destoryChunk(Chunk<byte[]> chunk)
	{
		
	}
	
	@Override
	public boolean isDirect()
	{
		return false;
	}
	
	@Override
	Chunk<byte[]> newChunk(int maxLevel, int pageSize, int pageSizeShift, int chunkSize)
	{
		return new HeapChunk(maxLevel, pageSize, pageSizeShift, chunkSize);
	}
	
	@Override
	Chunk<byte[]> newChunk(int reqCapacity)
	{
		return new HeapChunk(reqCapacity);
	}
	
	@Override
	void memoryCopy(byte[] src, int srcOffset, byte[] desc, int destOffset, int posi, int len)
	{
		System.arraycopy(src, srcOffset + posi, desc, destOffset + posi, len);
	}
	
}
