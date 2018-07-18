package com.jfireframework.jnet.common.util;

import java.nio.ByteBuffer;
import sun.nio.ch.DirectBuffer;

@SuppressWarnings("restriction")
public class PlatFormFunction
{
	public static long bytebufferOffsetAddress(ByteBuffer buffer)
	{
		return ((DirectBuffer) buffer).address();
	}
	
	public static sun.misc.Cleaner bytebufferCleaner(ByteBuffer buffer)
	{
		return ((DirectBuffer) buffer).cleaner();
	}
}
