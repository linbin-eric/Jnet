package com.jfireframework.jnet.common.internal;

import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import org.jctools.queues.MpscLinkedQueue7;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;

public class DefaultWriteCompleteHandler extends BindDownAndUpStreamDataProcessor implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET        = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD      = 128;
    protected static final int                       IDLE                = 1;
    protected static final int                       WORK                = 2;
    protected final        WriteEntry                entry               = new WriteEntry();
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        BufferAllocator           allocator;
    protected final        AioListener               aioListener;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state               = IDLE;
    protected              Queue<IoBuffer>           queue;

    public DefaultWriteCompleteHandler(AsynchronousSocketChannel socketChannel, AioListener aioListener, BufferAllocator allocator, int maxWriteBytes)
    {
        this.socketChannel = socketChannel;
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.maxWriteBytes = Math.max(1, maxWriteBytes);
        queue = new MpscLinkedQueue7<>();
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

    protected boolean changeToWork()
    {
        return UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
    }

    @Override
    public void completed(Integer result, WriteEntry entry)
    {
        try
        {
            aioListener.afterWrited(channelContext, result);
            ByteBuffer byteBuffer = entry.getByteBuffer();
            if (byteBuffer.hasRemaining())
            {
                socketChannel.write(byteBuffer, entry, this);
                return;
            }
            entry.clean();
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
        catch (Throwable e)
        {
            failed(e, entry);
        }
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
        entry.clean();
        prepareTermination();
        channelContext.close(exc);
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public void process(Object data) throws IllegalStateException
    {
        IoBuffer buf = (IoBuffer) data;
        if (buf == null)
        {
            throw new NullPointerException();
        }
        queue.offer(buf);
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

    protected void prepareTermination()
    {
        while (true)
        {
            IoBuffer tmp;
            while ((tmp = queue.poll()) != null)
            {
                tmp.free();
            }
            state = IDLE;
            if (queue.isEmpty())
            {
                break;
            }
            else if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
            {
                ;
            }
            else
            {
                break;
            }
        }
    }
}
