package com.jfirer.jnet.common.api;

public interface WriteProcessorNode
{
    void fireWrite(Object data);

    void fireChannelClosed();

    void fireWriteFailed(Throwable e);

    WriteProcessorNode getNext();

    void setNext(WriteProcessorNode next);

    Pipeline pipeline();
}
