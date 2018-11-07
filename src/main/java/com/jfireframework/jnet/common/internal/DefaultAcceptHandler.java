package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.BufferAllocator;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

public class DefaultAcceptHandler implements AcceptHandler
{
    protected final AioListener               aioListener;
    protected final BufferAllocator           allocator;
    protected final int                       maxWriteBytes;
    protected final ChannelContextInitializer channelContextInitializer;
    protected final BackPressureMode          backPressureMode;

    /**
     * @param aioListener
     * @param allocator
     * @param maxWriteBytes             单次最大写字节数，写完成器默认会不断聚合数据，直到达到最大写字节数或者聚合完毕，才会真正执行写操作
     * @param channelContextInitializer
     * @param backPressure
     * @param queueCapacity             背压模式时，写完成器的队列长度。
     */
    public DefaultAcceptHandler(AioListener aioListener, BufferAllocator allocator, int maxWriteBytes, ChannelContextInitializer channelContextInitializer, BackPressureMode backPressureMode)
    {
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.maxWriteBytes = maxWriteBytes;
        this.channelContextInitializer = channelContextInitializer;
        this.backPressureMode = backPressureMode;
    }

    public DefaultAcceptHandler(AioListener aioListener, BufferAllocator allocator, ChannelContextInitializer channelContextInitializer)
    {
        this(aioListener, allocator, 1, channelContextInitializer, new BackPressureMode());
    }

    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        WriteCompletionHandler writeCompletionHandler = new DefaultWriteCompleteHandler(socketChannel, aioListener, allocator, maxWriteBytes, backPressureMode);
        ReadCompletionHandler  readCompletionHandler  = new DefaultReadCompletionHandler(aioListener, allocator, socketChannel);
        ChannelContext         channelContext         = new DefaultChannelContext(socketChannel, aioListener, readCompletionHandler, writeCompletionHandler);
        if (aioListener != null)
        {
            aioListener.onAccept(socketChannel, channelContext);
        }
        channelContextInitializer.onChannelContextInit(channelContext);
        readCompletionHandler.start();
        serverChannel.accept(serverChannel, this);
    }

    @Override
    public void failed(Throwable exc, AsynchronousServerSocketChannel serverChannel)
    {
        try
        {
            serverChannel.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
