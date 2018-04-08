package com.jfireframework.jnet.common.mem.support;

import com.jfireframework.jnet.common.mem.buffer.IoBuffer;

public interface IoBufferAllocator
{
	IoBuffer allocate(int initSize);
	
	IoBuffer allocateDirect(int initSize);
	
	void release(IoBuffer ioBuffer);
}
