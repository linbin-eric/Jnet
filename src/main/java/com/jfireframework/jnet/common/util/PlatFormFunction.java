package com.jfireframework.jnet.common.util;

import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

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
	
	public static long bytebufferOffsetAddress(ByteBuffer buffer)
	{
		return ((DirectBuffer) buffer).address();
	}
}
