package com.jfireframework.jnet.common.util;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class PlatFormFunction
{
	private static final Unsafe UNSAFE;
	static
	{
		Object unsafe;
		try
		{
			unsafe = ReflectUtil.getUnsafe();
		}
		catch (Exception e)
		{
			unsafe = null;
		}
		UNSAFE = (Unsafe) unsafe;
	}
	
	static boolean hasUnsafe()
	{
		return UNSAFE != null;
	}
	
	public static void throwException(Throwable t)
	{
		if (hasUnsafe())
		{
			if (t == null)
			{
				throw new NullPointerException("传入的参数为null");
			}
			else
			{
				UNSAFE.throwException(t);
			}
		}
		else
		{
			PlatFormFunction.<RuntimeException> throwException0(t);
		}
	}
	
	@SuppressWarnings("unchecked")
	static <E extends Throwable> void throwException0(Throwable t) throws E
	{
		throw (E) t;
	}
}
