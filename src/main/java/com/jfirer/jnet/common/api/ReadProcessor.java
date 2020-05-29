package com.jfirer.jnet.common.api;

public interface ReadProcessor<T>
{
    void read(T data, ProcessorContext ctx);

    default void prepareFirstRead(ProcessorContext ctx)
    {
        ctx.firePrepareFirstRead();
    }

    default void channelClose(ProcessorContext ctx)
    {
        ctx.fireChannelClose();
    }

    default void exceptionCatch(Throwable e,ProcessorContext ctx)
    {
        ctx.fireExceptionCatch(e);
    }
}
