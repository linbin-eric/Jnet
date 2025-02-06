package com.jfirer.jnet.common.api;

public interface ReadProcessorNode
{
    void fireRead(Object data);

    void fireReadFailed(Throwable e);

    void firePipelineComplete(Pipeline pipeline);

    ReadProcessorNode getNext();

    void setNext(ReadProcessorNode next);

    Pipeline pipeline();
}
