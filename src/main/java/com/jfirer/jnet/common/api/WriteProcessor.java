package com.jfirer.jnet.common.api;

public interface WriteProcessor<T>
{
    default void write(T data, ProcessorContext prev)
    {
        prev.fireWrite(data);
    }

    default void endOfWriteLife(ProcessorContext prev)
    {
        prev.fireEndOfWriteLife();
    }
}
