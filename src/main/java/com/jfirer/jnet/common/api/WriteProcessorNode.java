package com.jfirer.jnet.common.api;

public interface WriteProcessorNode
{
    void fireWrite(Object data);

    void fireChannelClosed();

    void fireWriteFailed(Throwable e);

    void setNext(WriteProcessorNode next);

    WriteProcessorNode getNext();
}
