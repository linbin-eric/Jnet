package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;

public interface AioListener
{
    /**
     * 当数据被写出后触发
     * 
     * @param channelContext
     * @param writes
     */
    void afterWrited(AsynchronousSocketChannel socketChannel, Integer writes);
    
    void onAccept(AsynchronousSocketChannel socketChannel, ChannelContext channelContext);
    
    /**
     * 通道发生异常时触发
     * 
     * @param e
     * @param channelContext
     */
    void catchException(Throwable e, AsynchronousSocketChannel socketChannel);
    
    /**
     * 通道收到消息后触发
     * 
     * @param context
     */
    void afterReceived(AsynchronousSocketChannel socketChannel);
    
}
