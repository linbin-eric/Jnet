package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.concurrent.MPSCArrayQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.BackPressureCommonPool;

public class BackPressureWriteCompleteHandler extends AbstractWriteCompleteHandler
{
    private ReadCompletionHandler readCompletionHandler;
    
    public BackPressureWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes, int queueCapacity)
    {
        super(socketChannel, aioListener, allocator, maxWriteBytes);
        queue = new MPSCArrayQueue<IoBuffer>(Math.max(queueCapacity, 1024));
    }
    
    public BackPressureWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes)
    {
        this(socketChannel, aioListener, allocator, maxWriteBytes, 1024);
    }
    
    @Override
    public void offer(IoBuffer buf)
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean backPressureOffer(IoBuffer buf, boolean first) throws IllegalStateException
    {
        if (buf == null)
        {
            throw new NullPointerException();
        }
        if (first)
        {
            if (queue.offer(buf) == false)
            {
                BackPressureCommonPool.submit(buf, this, readCompletionHandler);
                return false;
            }
        }
        else
        {
            if (queue.offer(buf) == false)
            {
                return false;
            }
        }
        int now = state;
        if (now == TERMINATION)
        {
            throw new IllegalStateException("该通道已经处于关闭状态，无法执行写操作");
        }
        if (now == IDLE && changeToWork())
        {
            if (queue.isEmpty() == false)
            {
                writeQueuedBuffer();
            }
            else
            {
                rest();
            }
        }
        return true;
    }
    
    @Override
    public void bind(ReadCompletionHandler readCompletionHandler)
    {
        this.readCompletionHandler = readCompletionHandler;
    }
    
}
