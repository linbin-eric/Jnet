package com.jfireframework.jnet.common.mem.archon;

import com.jfireframework.jnet.common.mem.handler.Handler;

public abstract class UnPooledArchon<T> implements Archon<T>
{
	
	@Override
	public void recycle(Handler<T> handler)
	{
		
	}
	
}
