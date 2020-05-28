package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;

public interface ChannelContext
{
    ChannelConfig channelConfig();

    AsynchronousSocketChannel socketChannel();

    void close();

    void close(Throwable e);
}
