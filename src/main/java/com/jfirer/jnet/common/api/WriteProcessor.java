package com.jfirer.jnet.common.api;

public interface WriteProcessor<T>
{
    void write(T data,ProcessorContext ctx);
}
