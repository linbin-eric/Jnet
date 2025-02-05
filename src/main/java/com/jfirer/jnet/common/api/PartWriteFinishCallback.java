package com.jfirer.jnet.common.api;

import java.util.concurrent.atomic.AtomicLong;

public interface PartWriteFinishCallback
{
    PartWriteFinishCallback INSTANCE = new PartWriteFinishCallback()
    {
    };

    default void partWriteFinish(long queueCapacity) {}

    default void writeFailed(Throwable e, AtomicLong queueCapacity)
    {
    }
}
