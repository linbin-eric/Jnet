package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorContext;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.common.util.SpscLinkedQueue;
import com.jfireframework.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultWriteCompleteHandler implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET               = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD             = 16;
    protected static final int                       IDLE                       = 1;
    protected static final int                       WORK                       = 2;
    protected final        WriteEntry                entry                      = new WriteEntry();
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        ChannelContext            channelContext;
    protected final        BufferAllocator           allocator;
    protected final        AioListener               aioListener;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state                      = IDLE;
    protected              Queue<IoBuffer>           queue                      = new SpscLinkedQueue<>();
    protected              AtomicInteger             pendingWriteBytes          = new AtomicInteger();
    static final           long                      PENDING_WRITE_BYTES_OFFSET = UNSAFE.getFieldOffset("pendingWriteBytes");
    private final          Thread                    thread;

    public DefaultWriteCompleteHandler(ChannelContext channelContext, Thread thread)
    {
        this.thread = thread;
        this.socketChannel = channelContext.socketChannel();
        ChannelConfig channelConfig = channelContext.channelConfig();
        this.allocator = channelConfig.getAllocator();
        this.aioListener = channelConfig.getAioListener();
        this.maxWriteBytes = Math.max(1, channelConfig.getMaxBatchWrite());
        this.channelContext = channelContext;
        System.out.println("创建写出完成器");
    }

    @Override
    protected void finalize() throws Throwable
    {
        System.out.println("被回收");
        super.finalize();
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
//                if (pendingWriteBytes.get() == 0)
//                {
//                    System.err.println("异常情况,queue:" + queue.size());
//                    System.err.println("异常,buffer:" + queue.peek());
//                    int pe = 0;
//                    while (queue.peek() == null || (pe = pendingWriteBytes.get()) == 0)
//                    {
//                        System.err.println("奇怪：" + queue.peek() + ",pending:" + pe);
//                    }
//                    System.out.println("pe:" + pe);
//                }
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
//            if (pending <= 0)
//            {
//                System.err.println("pending:" + pending + ",max:" + maxBufferedCapacity + ",p:" + pendingWriteBytes.get());
//            }
            if (total == 0)
            {
                System.out.println("触发");
                total = 1024;
            }
            while (count < total && (buffer = queue.poll()) != null)
            {
                sum2.incrementAndGet();
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
        e.printStackTrace();
        channelContext.close(e);
        entry.clean();
        prepareTermination();
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

    private AtomicInteger sum  = new AtomicInteger();
    private AtomicInteger sum2 = new AtomicInteger();
    private int           sum3 = 0;

    @Override
    public void write(Object data, ProcessorContext ctx)
    {
        sum3++;
        sum.incrementAndGet();
        if (thread != Thread.currentThread())
        {
            System.err.println("线程异常");
        }
        IoBuffer buf = (IoBuffer) data;
        if (buf == null)
        {
            System.err.println("有空数据");
            throw new NullPointerException();
        }
        int size = buf.remainRead();
        int get  = pendingWriteBytes.addAndGet(size);
        if (get <= 0)
        {
            System.err.println("出现了空间为0的buffer");
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
}
