package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.arena.impl.SubPageImpl;

public interface SubPage
{
    static <T> SubPage newSubPage(int handle, int pageSize, int offset, T memory, Chunk chunk)
    {
        return new SubPageImpl<T>(chunk, pageSize, handle, offset);
    }

    void reset(int elementSize);

    long allocate();

    void free(int bitmapIdx);

    Chunk chunk();

    int handle();

    int index();

    /**
     * 该子页已经没有可以分配的空间
     *
     * @return
     */
    boolean empty();

    /**
     * 该子页所有等分区域均可用
     *
     * @return
     */
    boolean allAvail();

    boolean oneAvail();

    int elementSize();

    int numOfAvail();
}
