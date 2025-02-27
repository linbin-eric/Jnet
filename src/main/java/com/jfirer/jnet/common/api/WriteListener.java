package com.jfirer.jnet.common.api;

public interface WriteListener
{
    WriteListener INSTANCE = new WriteListener()
    {
    };

    /**
     * @param currentSend 本次已经写出的容量
     */
    default void partWriteFinish(long currentSend) {}

    /**
     * 当前入队的待写出大小
     *
     * @param size
     */
    default void queuedWrite(long size) {}

    default void writeFailed(Throwable e)
    {
    }
}
