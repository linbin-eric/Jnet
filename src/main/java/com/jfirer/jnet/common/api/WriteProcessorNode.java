package com.jfirer.jnet.common.api;

public interface WriteProcessorNode
{
    void fireWrite(Object data);

    void firePipelineComplete();

    void fireWriteClose();

    void setNext(WriteProcessorNode next);

    WriteProcessorNode next();

    JnetWorker worker();
}
