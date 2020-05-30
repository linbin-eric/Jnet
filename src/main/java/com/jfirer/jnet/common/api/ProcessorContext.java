package com.jfirer.jnet.common.api;

public interface ProcessorContext
{
    void fireRead(Object data);

    void fireWrite(Object data);

    void firePrepareFirstRead();

    void fireChannelClose();

    void fireExceptionCatch(Throwable e);

    void fireEndOfLife();

    ChannelContext channelContext();
}
