package com.jfirer.jnet.common.api;

public interface ReadProcessor<T>
{
    void read(T data, ProcessorContext next);

    default void prepareFirstRead(ProcessorContext next)
    {
        next.firePrepareFirstRead();
    }

    default void channelClose(ProcessorContext next)
    {
        next.fireChannelClose();
    }

    default void exceptionCatch(Throwable e, ProcessorContext next)
    {
        next.fireExceptionCatch(e);
    }

    default void endOfLife(ProcessorContext next)
    {
        next.fireEndOfLife();
    }
}
