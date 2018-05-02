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
	 * 将Bucket中的内存回收。<br/>
	 * 
	 * @param bucket
	 */
	void recycle(PooledIoBuffer buffer);
	
	void recycle(PooledIoBuffer[] buffers, int off, int len);
	
	/**
	 * 将buffer扩容到newSize大小。
	 * 
	 * @param buffer
	 * @param newSize
	 */
	void expansion(PooledIoBuffer buffer, int newSize);
}
