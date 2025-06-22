package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
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

    Object getAttach();

    void setAttach(Object attach);

    void setReadListener(ReadListener readListener);

    void setWriteListener(WriteListener writeListener);

    boolean isOpen();

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

    BufferAllocator allocator();
}
