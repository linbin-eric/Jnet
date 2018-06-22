package com.jfireframework.jnet.common.thread;

public class FastThreadLocalThread extends Thread
{
	private FastThreadLocalMap fastThreadLocalMap;
	
	public FastThreadLocalThread(Runnable runnable)
	{
		super(runnable);
	}
	
	public FastThreadLocalMap getIfHaveFastThreadLocalMap()
	{
		return fastThreadLocalMap;
	}
	
	public FastThreadLocalMap getOrInitializeFastThreadLocalMap()
	{
		FastThreadLocalMap fastThreadLocalMap = this.fastThreadLocalMap;
		if (fastThreadLocalMap == null)
		{
			this.fastThreadLocalMap = fastThreadLocalMap = new FastThreadLocalMap();
		}
		return fastThreadLocalMap;
	}
	
}
