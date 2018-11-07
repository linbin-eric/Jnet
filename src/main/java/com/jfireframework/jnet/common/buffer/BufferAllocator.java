package com.jfireframework.jnet.common.buffer;

public interface BufferAllocator
{
    /**
     * 不限定Buffer类型，自行决定
     *
     * @return
     */
    IoBuffer ioBuffer(int initializeCapacity);

    IoBuffer ioBuffer(int initializeCapacity, boolean direct);

    IoBuffer heapBuffer(int initializeCapacity);

    IoBuffer directBuffer(int initializeCapacity);

    String name();
}
