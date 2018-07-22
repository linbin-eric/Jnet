package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import com.jfireframework.baseutil.concurrent.MPSCArrayQueue;
import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.BackPressureCommonPool;

public class BackPressureWriteCompleteHandler implements WriteCompletionHandler
{
    protected static final long               STATE_OFFSET        = UNSAFE.getFieldOffset("state", AbstractWriteCompletionHandler.class);
    protected static final int                SPIN_THRESHOLD      = 1 << 7;
    protected static final int                WORK                = 1;
    protected static final int                IDLE                = 2;
    // 终止状态位，也就是负数标识位
    protected static final int                TERMINATION         = 3;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile int                    state               = IDLE;
    protected Queue<IoBuffer>                 queue;
    protected final WriteEntry                entry               = new WriteEntry();
    protected final AsynchronousSocketChannel socketChannel;
    protected final BufferAllocator           allocator;
    protected final AioListener               aioListener;
    private ReadCompletionHandler             readCompletionHandler;
    private int                               maxBufferedCapacity = 1024 * 1024;
    
    public BackPressureWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int queueCapacity)
    {
        this.socketChannel = socketChannel;
        this.allocator = allocator;
        this.aioListener = aioListener;
        queue = new MPSCArrayQueue<IoBuffer>(Math.max(queueCapacity, 4096));
    }
    
    @Override
    public void offer(IoBuffer buf)
    {
        throw new UnsupportedOperationException();
    }
    
    protected void rest()
    {
        state = IDLE;
        if (queue.isEmpty() == false)
        {
            int now = state;
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
    }
    
    @Override
    public void completed(Integer result, WriteEntry entry)
    {
        if (aioListener != null)
        {
            try
            {
                aioListener.afterWrited(socketChannel, result);
            }
            catch (Throwable e)
            {
                ;
            }
        }
        ByteBuffer byteBuffer = entry.getByteBuffer();
        if (byteBuffer.hasRemaining())
        {
            socketChannel.write(byteBuffer, entry, this);
            return;
        }
        entry.getIoBuffer().free();
        entry.clear();
        if (queue.isEmpty() == false)
        {
            writeQueuedBuffer();
            return;
        }
        for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
        {
            if (queue.isEmpty() == false)
            {
                writeQueuedBuffer();
                return;
            }
        }
        rest();
    }
    
    /**
     * 从MPSCQueue中取得IoBuffer，并且执行写操作
     */
    protected void writeQueuedBuffer()
    {
        IoBuffer head = null;
        int maxBufferedCapacity = this.maxBufferedCapacity;
        int count = 0;
        IoBuffer buffer;
        while (count < maxBufferedCapacity && (buffer = queue.poll()) != null)
        {
            count += buffer.remainRead();
            if (head == null)
            {
                head = buffer;
            }
            else
            {
                head.put(buffer);
                buffer.free();
            }
        }
        entry.setIoBuffer(head);
        entry.setByteBuffer(head.readableByteBuffer());
        socketChannel.write(entry.getByteBuffer(), entry, this);
    }
    
    protected boolean changeToWork()
    {
        return UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
    }
    
    @Override
    public void failed(Throwable exc, WriteEntry entry)
    {
        if (aioListener != null)
        {
            aioListener.catchException(exc, socketChannel);
        }
        state = TERMINATION;
        entry.getIoBuffer().free();
        entry.clear();
        while (queue.isEmpty() == false)
        {
            queue.poll().free();
        }
        try
        {
            socketChannel.close();
        }
        catch (IOException e)
        {
            ;
        }
    }
    
    @Override
    public boolean backpressureOffer(IoBuffer buf, boolean first) throws IllegalStateException
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
