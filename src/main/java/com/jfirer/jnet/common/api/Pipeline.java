package com.jfirer.jnet.common.api;

public interface Pipeline
{
    void add(Object readProcessor);

    void add(Object readProcessor, WorkerGroup workerGroup);

    void fireRead(Object data);

    void fireWrite(Object data);

    void fireChannelClose();

    void fireExceptionCatch(Throwable e);

    void firePrepareFirstRead();
}
