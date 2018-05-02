package com.jfireframework.jnet.common.buffer;

public interface Archon
{
	/**
	 * 申请一段内存。如果申请失败则返回false <br/>
	 * 
	 * @param need
	 * @param bucket
	 * @return
	 */
	void apply(int need, PooledIoBuffer buffer);
	
	/**
	 * chunk中的下标为index处的内存区域可以回收
	 * 
	 * @param chunk
	 * @param indexOfChunk
	 */
	void recycle(Chunk chunk, int index);
	
	/**
	 * 将buffer扩容到newSize大小。
	 * 
	 * @param buffer
	 * @param newSize
	 */
	void expansion(PooledIoBuffer buffer, int newSize);
}
