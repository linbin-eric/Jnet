package com.jfireframework.jnet.common.api;

public interface Pipeline
{
    void add(Object readProcessor);

    void add(Object readProcessor, WorkerGroup workerGroup);

    void read(Object data);

    void write(Object data);
}
