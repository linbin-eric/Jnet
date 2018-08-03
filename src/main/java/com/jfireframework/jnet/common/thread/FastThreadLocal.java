package com.jfireframework.jnet.common.thread;

import java.util.concurrent.atomic.AtomicInteger;

public class FastThreadLocal<T>
{
	// 下标0用作特殊用途，暂时保留
	static final AtomicInteger	IDGENERATOR	= new AtomicInteger(1);
	private final int			idx			= IDGENERATOR.getAndIncrement();
	
	@SuppressWarnings("unchecked")
	public T get()
	{
		FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.get();
		T result = (T) fastThreadLocalMap.get(idx);
		if (result != null)
		{
			return result;
		}
		result = initializeValue();
		if (result == null)
		{
			return null;
		}
		else
		{
			fastThreadLocalMap.set(result, idx);
			return result;
		}
	}
	
	/**
	 * 如果当前线程变量中没有这个值则调用该方法进行初始化
	 * 
	 * @return
	 */
	protected T initializeValue()
	{
		return null;
	}
	
	public void remove()
	{
		FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.getIfSet();
		if (fastThreadLocalMap != null)
		{
			fastThreadLocalMap.remove(idx);
		}
	}
	
	public void set(T value)
	{
		FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.get();
		fastThreadLocalMap.set(value, idx);
	}
}
