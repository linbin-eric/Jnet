package com.jfirer.jnet.common.api;

public interface ReadProcessorNode
{
    void fireRead(Object data);

    void firePipelineComplete();

    void fireExceptionCatch(Throwable e);

    void fireReadClose();

    void fireChannelClose();

    void setNext(ReadProcessorNode next);

    ReadProcessorNode next();

    Pipeline pipeline();

    JnetWorker worker();
}
