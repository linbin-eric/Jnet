package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.common.util.UNSAFE;
import org.jctools.queues.MpscLinkedQueue8;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;

public class DefaultWriteCompleteHandler extends BindDownAndUpStreamDataProcessor implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET               = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD             = 16;
    protected static final int                       IDLE                       = 1;
    protected static final int                       WORK                       = 2;
    protected final        WriteEntry                entry                      = new WriteEntry();
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        BufferAllocator           allocator;
    protected final        AioListener               aioListener;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state                      = IDLE;
    protected              Queue<IoBuffer>           queue;
    protected volatile     int                       pendingWriteBytes;
    static final           long                      PENDING_WRITE_BYTES_OFFSET = UNSAFE.getFieldOffset("pendingWriteBytes");

    public DefaultWriteCompleteHandler(ChannelConfig channelConfig, AsynchronousSocketChannel socketChannel)
    {
        this.socketChannel = socketChannel;
        this.allocator = channelConfig.getAllocator();
        this.aioListener = channelConfig.getAioListener();
        this.maxWriteBytes = Math.max(1, channelConfig.getMaxBatchWrite());
        queue = new MpscLinkedQueue8<>();
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
        int      maxBufferedCapacity = this.maxWriteBytes;
        int      count               = 0;
        IoBuffer buffer;
        IoBuffer accumulation        = null;
        boolean  create              = false;
        int      pending             = pendingWriteBytes;
        int      total               = pending > maxBufferedCapacity ? maxBufferedCapacity : pending;
        while (count < total && (buffer = queue.poll()) != null)
        {
            count += buffer.remainRead();
            if (accumulation == null)
            {
                accumulation = buffer;
            }
            else
            {
                if (create == false)
                {
                    create = true;
                    IoBuffer newAcc = allocator.ioBuffer(total);
                    newAcc.put(accumulation);
                    accumulation.free();
                    accumulation = newAcc;
                }
                accumulation.put(buffer);
                buffer.free();
            }
        }
        addPendingWriteBytes(0 - accumulation.remainRead());
        entry.setIoBuffer(accumulation);
        entry.setByteBuffer(accumulation.readableByteBuffer());
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
        addPendingWriteBytes(buf.remainRead());
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

    int addPendingWriteBytes(int add)
    {
        int current = pendingWriteBytes;
        int update  = current + add;
        if (UNSAFE.compareAndSwapInt(this, PENDING_WRITE_BYTES_OFFSET, current, update))
        {
            return update;
        }
        do
        {
            current = pendingWriteBytes;
            update = current + add;
        } while (UNSAFE.compareAndSwapInt(this, PENDING_WRITE_BYTES_OFFSET, current, update) == false);
        return update;
    }
}
