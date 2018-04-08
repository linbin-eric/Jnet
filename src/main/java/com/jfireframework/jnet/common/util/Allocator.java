package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.mem.buffer.IoBuffer;
import com.jfireframework.jnet.common.mem.support.IoBufferAllocator;
import com.jfireframework.jnet.common.mem.support.PooledIoBufferAllocator;
import com.jfireframework.jnet.common.mem.support.UnPooledIoBufferAllocator;

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
