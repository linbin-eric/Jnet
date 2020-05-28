package com.jfirer.jnet.common.api;

public interface ReadProcessor<T>
{
    void read(T data,ProcessorContext ctx);
}
