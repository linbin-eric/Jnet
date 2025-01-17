package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.exception.SelfCloseException;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;

public interface Pipeline
{
    void fireWrite(Object data);

    void addReadProcessor(ReadProcessor<?> processor);

    void addWriteProcessor(WriteProcessor<?> processor);

    default void close()
    {
        close(new SelfCloseException());
    }

    void close(Throwable e);

    AsynchronousSocketChannel socketChannel();

    ChannelConfig channelConfig();

    static void invokeMethodIgnoreException(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable e)
        {
            ;
        }
    }
}
