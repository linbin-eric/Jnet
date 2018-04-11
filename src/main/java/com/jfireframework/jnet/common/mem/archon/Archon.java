package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.IoBuffer;

public interface Archon
{
	/**
	 * 申请一段内存。如果申请失败则返回false <br/>
	 * 
	 * @param need
	 * @param bucket
	 * @return
	 */
	void apply(int need, IoBuffer buffer);
	
	/**
	 * 将Bucket中的内存回收。<br/>
	 * 
	 * @param bucket
	 */
	void recycle(IoBuffer buffer);
	
	/**
	 * 对handler进行扩容，扩容流程是先申请一个newSize大小的空间，将handler本身的内容复制过去。然后将handler中的部分回收。<br/>
	 * 
	 * @param buffer
	 * @param newSize
	 */
	void expansion(IoBuffer buffer, int newSize);
}
