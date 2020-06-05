package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ProcessorContext;
import com.jfirer.jnet.common.api.WriteCompletionHandler;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.SpscLinkedQueue;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultWriteCompleteHandler implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET      = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD    = 16;
    protected static final int                       IDLE              = 1;
    protected static final int                       WORK              = 2;
    protected final        WriteEntry                entry             = new WriteEntry();
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        ChannelContext            channelContext;
    protected final        BufferAllocator           allocator;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state             = IDLE;
    //注意，不能使用JcTools下面的SpscQueue，其实现会出现当queue.isEmpty()==false时，queue.poll()返回null，导致程序异常
    //MpscQueue则是可以的。JDK的并发queue也是可以的
    protected              Queue<IoBuffer>           queue             = new SpscLinkedQueue<>();
    protected              AtomicInteger             pendingWriteBytes = new AtomicInteger();

    public DefaultWriteCompleteHandler(ChannelContext channelContext)
    {
        this.socketChannel = channelContext.socketChannel();
        ChannelConfig channelConfig = channelContext.channelConfig();
        this.allocator = channelConfig.getAllocator();
        this.maxWriteBytes = Math.max(1, channelConfig.getMaxBatchWrite());
        this.channelContext = channelContext;
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
        try
        {
            int      maxBufferedCapacity = this.maxWriteBytes;
            int      count               = 0;
            IoBuffer buffer              = null;
            IoBuffer accumulation        = null;
            boolean  create              = false;
            int      pending             = pendingWriteBytes.get();
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
            int addAndGet = pendingWriteBytes.addAndGet(0 - accumulation.remainRead());
            if (addAndGet < 0)
            {
                System.err.println("小于0：" + addAndGet);
            }
            entry.setIoBuffer(accumulation);
            entry.setByteBuffer(accumulation.readableByteBuffer());
            socketChannel.write(entry.getByteBuffer(), entry, this);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Throwable e, WriteEntry entry)
    {
        Pipeline pipeline = channelContext.pipeline();
        try
        {
            pipeline.fireExceptionCatch(e);
            channelContext.close(e);
        }
        finally
        {
            entry.clean();
            prepareTermination();
            pipeline.fireEndOfWriteLife();
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

    @Override
    public void write(Object data, ProcessorContext ctx)
    {
        IoBuffer buf = (IoBuffer) data;
        if (buf == null)
        {
            throw new NullPointerException();
        }
        int size = buf.remainRead();
        pendingWriteBytes.addAndGet(size);
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

    @Override
    public void endOfWriteLife(ProcessorContext prev)
    {
    }
}
