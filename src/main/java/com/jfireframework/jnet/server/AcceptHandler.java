package com.jfireframework.jnet.server;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.internal.BatchWriteCompletionHandler;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;
import com.jfireframework.jnet.common.internal.SingleWriteCompletionHandler;

public abstract class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
    protected final AioListener     aioListener;
    protected final BufferAllocator allocator;
    protected final int             batchWriteNum;
    protected final int             writeQueueCapacity;
    
    public AcceptHandler(AioListener aioListener, BufferAllocator allocator, int batchWriteNum, int writeQueueCapacity)
    {
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.batchWriteNum = batchWriteNum;
        this.writeQueueCapacity = writeQueueCapacity;
    }
    
    public AcceptHandler(AioListener aioListener, BufferAllocator allocator)
    {
        this(aioListener, allocator, 1, 512);
    }
    
    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        WriteCompletionHandler writeCompletionHandler = batchWriteNum <= 1 ? new SingleWriteCompletionHandler(socketChannel, aioListener, allocator, writeQueueCapacity) : new BatchWriteCompletionHandler(aioListener, socketChannel, allocator, writeQueueCapacity, batchWriteNum);
        ChannelContext channelContext = new DefaultChannelContext(socketChannel, aioListener);
        ReadCompletionHandler readCompletionHandler = new DefaultReadCompletionHandler(aioListener, allocator, socketChannel);
        readCompletionHandler.bind(channelContext);
        channelContext.bindWriteCompleteHandler(writeCompletionHandler);
        if (aioListener != null)
        {
            aioListener.onAccept(socketChannel, channelContext);
        }
        onChannelContextInit(channelContext);
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
    
    protected abstract void onChannelContextInit(ChannelContext channelContext);
    
}
