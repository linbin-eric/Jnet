package com.jfireframework.jnet.common.buffer;

public interface BufferAllocator
{
	IoBuffer heapBuffer(int initializeCapacity);
	
	IoBuffer directBuffer(int initializeCapacity);
}
