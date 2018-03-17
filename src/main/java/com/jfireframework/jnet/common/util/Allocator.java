package com.jfireframework.jnet.common.util;

import com.jfireframework.pool.IoBufferAllocator;
import com.jfireframework.pool.ioBuffer.IoBuffer;
import com.jfireframework.pool.support.PooledIoBufferAllocator;
import com.jfireframework.pool.support.UnPooledIoBufferAllocator;

public class Allocator
{
	private static IoBufferAllocator allocator = new PooledIoBufferAllocator();
	
	public static IoBuffer allocate(int initSize)
	{
		return allocator.allocate(initSize);
	}
	
	public static IoBuffer allocateDirect(int initSize)
	{
		return allocator.allocateDirect(initSize);
	}
}
