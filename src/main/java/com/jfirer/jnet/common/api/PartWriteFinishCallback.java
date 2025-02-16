package com.jfirer.jnet.common.api;

public interface PartWriteFinishCallback
{
    PartWriteFinishCallback INSTANCE = new PartWriteFinishCallback()
    {
    };

    /**
     * @param currentSend  本次已经写出的容量
     */
    default void partWriteFinish( long currentSend) {}

    default void writeFailed(Throwable e)
    {
    }
}
