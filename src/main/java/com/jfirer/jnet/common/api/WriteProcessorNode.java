package com.jfirer.jnet.common.api;

public interface WriteProcessorNode
{
    void fireWrite(Object data);

    void fireWriteClose();

    void setNext(WriteProcessorNode next);

    WriteProcessorNode next();
}
