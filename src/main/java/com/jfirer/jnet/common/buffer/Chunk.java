package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;

public interface Chunk<T>
{
    int usage();

    int getFreeBytes();

    int getChunkSize();

    int pageSize();

    int maxLevle();

    /**
     * 在chunk中的内存区域信息
     *
     * @param handle   该内存区域的下标节点
     * @param capacity 该内存区域的大小
     * @param offset   该内存区域
     */
    record MemoryArea<T>(int handle, int capacity, int offset, T memory, Chunk chunk) {}

    /**
     * 分配一个规范化后的容量大小的内存空间，返回该内存空间对应的信息。
     *
     * @param normalizeCapacity
     * @return
     */
    MemoryArea<T> allocate(int normalizeCapacity);

    /**
     * 分配一个内存页的空间，并且返回对应的SubPage对象
     *
     * @return
     */
    SubPage allocateSubpage();

    /**
     * 释放handle对应内存空间的占用。
     *
     * @param handle
     */
    void free(int handle);

    T memory();

    long directChunkAddress();

    boolean isUnPooled();

    static <T> void destory(Chunk<T> chunk)
    {
        if (chunk.memory() instanceof ByteBuffer byteBuffer && byteBuffer.isDirect())
        {
            UNSAFE.freeMemory(byteBuffer);
        }
    }
}
