package com.jfirer.jnet.common.api;

public interface InternalPipeline extends Pipeline
{
    void fireRead(Object data);

    void fireChannelClose(Throwable e);

    void fireExceptionCatch(Throwable e);

    void complete();

    void fireReadClose();

    void fireWriteClose();
}
