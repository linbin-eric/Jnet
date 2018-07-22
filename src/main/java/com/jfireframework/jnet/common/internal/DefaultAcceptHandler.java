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

public class DefaultAcceptHandler implements AcceptHandler
{
    protected final AioListener               aioListener;
    protected final BufferAllocator           allocator;
    protected final int                       maxWriteBytes;
    protected final int                       queueCapacity;
    protected final ChannelContextInitializer channelContextInitializer;
    protected final boolean                   backPressure;
    
    /**
     * 
     * @param aioListener
     * @param allocator
     * @param maxWriteBytes 单次最大写字节数，写完成器默认会不断聚合数据，直到达到最大写字节数或者聚合完毕，才会真正执行写操作
     * @param channelContextInitializer
     * @param backPressure
     * @param queueCapacity 背压模式时，写完成器的队列长度。
     */
    public DefaultAcceptHandler(AioListener aioListener, BufferAllocator allocator, int maxWriteBytes, ChannelContextInitializer channelContextInitializer, boolean backPressure, int queueCapacity)
    {
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.maxWriteBytes = maxWriteBytes;
        this.queueCapacity = queueCapacity;
        this.channelContextInitializer = channelContextInitializer;
        this.backPressure = backPressure;
    }
    
    public DefaultAcceptHandler(AioListener aioListener, BufferAllocator allocator, ChannelContextInitializer channelContextInitializer)
    {
        this(aioListener, allocator, 1, channelContextInitializer, false, -1);
    }
    
    @Override
    public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverChannel)
    {
        WriteCompletionHandler writeCompletionHandler = backPressure ? new BackPressureWriteCompleteHandler(socketChannel, aioListener, allocator, maxWriteBytes, queueCapacity) : new DefaultWriteCompletionHandler(socketChannel, aioListener, allocator, maxWriteBytes);
        ChannelContext channelContext = new DefaultChannelContext(socketChannel, aioListener);
        ReadCompletionHandler readCompletionHandler = new DefaultReadCompletionHandler(aioListener, allocator, socketChannel, backPressure);
        readCompletionHandler.bind(channelContext);
        channelContext.bind(writeCompletionHandler);
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
