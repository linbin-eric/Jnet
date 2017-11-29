package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelConnectListener
{
    /**
     * 当连接建立时触发
     */
    ChannelContext onConnect(AsynchronousSocketChannel socketChannel, AioListener aioListener);
    
}
