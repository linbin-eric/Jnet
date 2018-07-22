package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AcceptHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;

public class BackPressureAcceptHandler implements AcceptHandler
{
    protected final AioListener               aioListener;
    protected final BufferAllocator           allocator;
    protected final int                       batchWriteNum;
    protected final int                       writeQueueCapacity;
    protected final ChannelContextInitializer channelContextInitializer;
    
    public BackPressureAcceptHandler(AioListener aioListener, BufferAllocator allocator, int batchWriteNum, int writeQueueCapacity, ChannelContextInitializer channelContextInitializer)
    {
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.batchWriteNum = batchWriteNum;
        this.writeQueueCapacity = writeQueueCapacity;
        this.channelContextInitializer = channelContextInitializer;
    }
    
    public BackPressureAcceptHandler(AioListener aioListener, BufferAllocator allocator, ChannelContextInitializer channelContextInitializer)
    {
        this(aioListener, allocator, 1, 512, channelContextInitializer);
    }
    
    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        WriteCompletionHandler writeCompletionHandler = new BackPressureWriteCompleteHandler(socketChannel, aioListener, allocator, writeQueueCapacity);
        ChannelContext channelContext = new DefaultChannelContext(socketChannel, aioListener);
        ReadCompletionHandler readCompletionHandler = new BackPressureReadCompletionHandler(aioListener, allocator, socketChannel);
        readCompletionHandler.bind(channelContext);
        channelContext.bindWriteCompleteHandler(writeCompletionHandler);
        writeCompletionHandler.bind(readCompletionHandler);
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
