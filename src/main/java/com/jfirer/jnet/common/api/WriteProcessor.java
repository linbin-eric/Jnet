package com.jfirer.jnet.common.api;

public interface WriteProcessor<T>
{
    default void write(T data, WriteProcessorNode next)
    {
        next.fireWrite(data);
    }

    default void writeClose(WriteProcessorNode next)
    {
        next.fireWriteClose();
    }
}
