package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;

public interface Pipeline
{
    void fireWrite(Object data);

    void addReadProcessor(ReadProcessor<?> processor);

    void addWriteProcessor(WriteProcessor<?> processor);

    void shutdownInput();

    AsynchronousSocketChannel socketChannel();

    ChannelConfig channelConfig();

    void setAttach(Object attach);

    Object getAttach();

    /**
     * 当前通道的队列中的数据量
     *
     * @return
     */
    long writeQueueCapacity();

    void setRegisterReadCallback(RegisterReadCallback registerReadCallback);

    void setPartWriteFinishCallback(PartWriteFinishCallback partWriteFinishCallback);


    default String getRemoteAddressWithoutException()
    {
        try
        {
            return socketChannel().getRemoteAddress().toString();
        }
        catch (Throwable e)
        {
            return null;
        }
    }
}
