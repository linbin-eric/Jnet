package com.jfireframework.jnet.common.internal;

import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.BackPressureMode;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import org.jctools.queues.ConcurrentCircularArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.MpscLinkedQueue7;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;

public class DefaultWriteCompleteHandler extends BindDownAndUpStreamDataProcessor implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET   = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD = 128;
    protected static final int                       WORK           = 1;
    protected static final int                       IDLE           = 2;
    // 终止状态位，也就是负数标识位
    protected static final int                       TERMINATION    = 3;
    protected final        WriteEntry                entry          = new WriteEntry();
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        BufferAllocator           allocator;
    protected final        AioListener               aioListener;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state          = IDLE;
    protected              Queue<IoBuffer>           queue;
    private                ChannelContext            channelContext;
    private final          boolean                   boundary;
    private final          int                       boundarySize;

    public DefaultWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes, BackPressureMode backPressureMode)
    {
        this.socketChannel = socketChannel;
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.maxWriteBytes = Math.max(1, maxWriteBytes);
        queue = backPressureMode.isEnable() ? new MpscArrayQueue<IoBuffer>(backPressureMode.getQueueCapacity()) : new MpscLinkedQueue7<IoBuffer>();
        boundary = backPressureMode.isEnable();
        boundarySize = backPressureMode.isEnable() ? backPressureMode.getQueueCapacity() : 0;
    }

    protected void rest()
    {
        state = IDLE;
        try
        {
            upStream.notifyedWriterAvailable();
        } catch (Throwable throwable)
        {
            channelContext.close(throwable);
            return;
        }
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

    protected boolean changeToWork()
    {
        return UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
    }

    @Override
    public void completed(Integer result, WriteEntry entry)
    {
        if (aioListener != null)
        {
            try
            {
                aioListener.afterWrited(socketChannel, result);
            } catch (Throwable e)
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
        try
        {
            upStream.notifyedWriterAvailable();
        } catch (Throwable e)
        {
            channelContext.close(e);
            return;
        }
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
    private void writeQueuedBuffer()
    {
        IoBuffer head                = null;
        int      maxBufferedCapacity = this.maxWriteBytes;
        int      count               = 0;
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
        } catch (IOException e)
        {
            ;
        }
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(Object data) throws IllegalStateException
    {
        IoBuffer buf = (IoBuffer) data;
        if (buf == null)
        {
            throw new NullPointerException();
        }
        if (queue.offer(buf) == false)
        {
            return false;
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
    public void notifyedWriterAvailable()
    {
    }

    @Override
    public boolean canAccept()
    {
        if (boundary == false)
        {
            return true;
        }
        ConcurrentCircularArrayQueue<?> mpscArrayQuyeue = (ConcurrentCircularArrayQueue<?>) queue;
        long                            consumerIndex   = mpscArrayQuyeue.currentConsumerIndex();
        long                            producerIndex   = mpscArrayQuyeue.currentProducerIndex();
        return producerIndex - consumerIndex < boundarySize;
    }

    @Override
    public boolean isBoundary()
    {
        return boundary;
    }
}
