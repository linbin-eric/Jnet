package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelContext
{
    ChannelConfig channelConfig();

    AsynchronousSocketChannel socketChannel();

    void close();

    void close(Throwable e);
}
