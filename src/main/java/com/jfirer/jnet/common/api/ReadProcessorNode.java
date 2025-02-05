package com.jfirer.jnet.common.api;

public interface ReadProcessorNode
{
    void fireRead(Object data);

    void fireReadFailed(Throwable e);

    void firePipelineComplete(Pipeline pipeline);

    void setNext(ReadProcessorNode next);

    ReadProcessorNode getNext();

    Pipeline pipeline();
}
