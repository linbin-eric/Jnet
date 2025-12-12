package cc.jfire.jnet.common.buffer.allocator;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

public interface BufferAllocator
{
    /**
     * 不限定Buffer类型，自行决定
     *
     * @return
     */
    IoBuffer allocate(int initializeCapacity);

    void reAllocate(int initializeCapacity, IoBuffer buffer2);

    default String name()
    {
        return this.getClass().getName();
    }

    IoBuffer bufferInstance();

    void cycleBufferInstance(IoBuffer buffer);
}
