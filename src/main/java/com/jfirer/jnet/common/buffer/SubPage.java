package com.jfirer.jnet.common.buffer;

public interface SubPage
{
    static <T> SubPage newSubPage(int handle, int capacity, int offset, T memory, Chunk chunk)
    {
        return null;
    }

    void reset(int elementSize);

    long allocate();

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

    int getElementSize();
}
