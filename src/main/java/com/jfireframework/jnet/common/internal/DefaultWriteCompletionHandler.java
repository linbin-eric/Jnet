package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.concurrent.MPSCLinkedQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultWriteCompletionHandler extends AbstractWriteCompleteHandler
{
    
    public DefaultWriteCompletionHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes)
    {
        super(socketChannel, aioListener, allocator, maxWriteBytes);
        queue = new MPSCLinkedQueue<IoBuffer>();
    }
    
    @Override
    public void offer(IoBuffer buf)
    {
        if (buf == null)
        {
            throw new NullPointerException();
        }
        if (queue.offer(buf) == false)
        {
            while (queue.offer(buf) == false)
            {
                Thread.yield();
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
    }
    
    @Override
    public boolean backPressureOffer(IoBuffer buffer, boolean first) throws IllegalStateException
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void bind(ReadCompletionHandler readCompletionHandler)
    {
        ;
    }
}
