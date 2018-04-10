package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public interface Archon<T>
{
	/**
	 * 申请一段内存。如果申请失败则返回false <br/>
	 * 注意：该实现需要由SYNC关键字保护
	 * 
	 * @param need
	 * @param bucket
	 * @return
	 */
	void apply(int need, IoBuffer<T> handler);
	
	/**
	 * 将Bucket中的内存回收。<br/>
	 * 注意：该实现需要由SYNC关键字保护
	 * 
	 * @param bucket
	 */
	void recycle(IoBuffer<T> handler);
	
	/**
	 * 对handler进行扩容，扩容流程是先申请一个newSize大小的空间，将handler本身的内容复制过去。然后将handler中的部分回收。<br/>
	 * 注意：该实现需要有SYNC关键字保护
	 * 
	 * @param handler
	 * @param newSize
	 */
	void expansion(IoBuffer<T> handler, int newSize);
}
