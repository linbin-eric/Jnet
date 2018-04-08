package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.Handler;

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
	void apply(int need, Handler<T> handler);
	
	/**
	 * 将Bucket中的内存回收。<br/>
	 * 注意：该实现需要由SYNC关键字保护
	 * 
	 * @param bucket
	 */
	void recycle(Handler<T> handler);
}
