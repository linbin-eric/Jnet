package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.InternalPipeline;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteCompletionHandler;
import cc.jfire.jnet.common.api.WriteListener;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.exception.EndOfStreamException;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.common.util.UNSAFE;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscLinkedQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DefaultWriteCompleteHandler extends AtomicInteger implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET   = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD = 16;
    protected static final int                       OPEN_IDLE      = 0b00;
    protected static final int                       OPEN_WORK      = 0b01;
    protected static final int                       NOTICE_IDLE    = 0b10;
    protected static final int                       NOTICE_WORK    = 0b11;
    public static final    int                       OPEN           = 1;
    public static final    int                       CLOSED         = 0;
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        InternalPipeline          pipeline;
    protected final        BufferAllocator           allocator;
    // 终止状态。进入该状态后，不再继续使用
    protected final        int                       maxWriteBytes;
    /// /////////////////////////////////////////////////////////
    protected volatile     int                       state          = OPEN_IDLE;
    //注意，JcTools旧版本的SpscQueue，其实现会出现当queue.isEmpty()==false时，queue.poll()返回null，导致程序异常
    //MpscQueue则是可以的。JDK的并发queue也是可以的
    protected              Queue<IoBuffer>           queue;
    @Setter
    private                WriteListener             writeListener  = WriteListener.INSTANCE;
    private                IoBuffer                  sendingData;

    public DefaultWriteCompleteHandler(Pipeline pipeline)
    {
        this.pipeline      = (InternalPipeline) pipeline;
        this.socketChannel = pipeline.socketChannel();
        ChannelConfig channelConfig = pipeline.channelConfig();
        this.allocator     = pipeline.allocator();
        this.maxWriteBytes = Math.max(1024 * 1024, channelConfig.getMaxBatchWrite());
        queue              = new MpscLinkedQueue<>();
        set(OPEN);
    }

    @Override
    public void write(IoBuffer buffer)
    {
        try
        {
            if (buffer == null)
            {
                throw new NullPointerException();
            }
            writeListener.queuedWrite(buffer.remainRead());
            queue.offer(buffer);
            tryWork();
        }
        catch (Throwable e)
        {
            log.error("发生未预料异常", e);
            System.exit(108);
        }
    }

    public void noticeClose()
    {
        while (true)
        {
            int now = state;
            switch (now)
            {
                case OPEN_IDLE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, OPEN_IDLE, NOTICE_IDLE))
                    {
                        //通知成功，尝试进入工作状态来关闭
                        tryWork();
                        return;
                    }
                    else
                    {
                        //通知失败，继续尝试通知
                    }
                }

                case OPEN_WORK ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, OPEN_WORK, NOTICE_WORK))
                    {
                        //通知成功，别的线程已经在工作中，交给该线程即可。
                        return;
                    }
                    else
                    {
                        //通知失败，继续尝试通知
                    }
                }
                case NOTICE_IDLE ->
                {
                    tryWork();
                    return;
                }
                case NOTICE_WORK -> {return;}
            }
        }
    }

    protected void tryWork()
    {
        int now = state;
        switch (now)
        {
            case OPEN_IDLE ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, OPEN_IDLE, OPEN_WORK))
                {
                    if (queue.isEmpty())
                    {
                        quitToIdle();
                    }
                    else
                    {
                        writeQueuedBuffer();
                    }
                }
            }
            case NOTICE_IDLE ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, NOTICE_IDLE, NOTICE_WORK))
                {
                    if (queue.isEmpty())
                    {
                        closeChannel(new EndOfStreamException());
                        quitToIdle();
                    }
                    else
                    {
                        writeQueuedBuffer();
                    }
                }
            }
            case NOTICE_WORK, OPEN_WORK -> {;}
        }
    }

    private void closeChannel(Throwable e)
    {
        if (get() == CLOSED)
        {
            return;
        }
        if (compareAndSet(OPEN, CLOSED))
        {
            try
            {
                socketChannel.close();
            }
            catch (IOException ignored)
            {
                ;
            }
            pipeline.fireWriteFailed(e);
        }
    }

    protected void quitToIdle()
    {
        int now = state;
        switch (now)
        {
            case OPEN_WORK ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, OPEN_WORK, OPEN_IDLE))
                {
                    if (queue.isEmpty())
                    {
                        ;
                    }
                    else
                    {
                        tryWork();
                    }
                }
                else
                {
                    now = state;
                    if (now != NOTICE_WORK)
                    {
                        log.error("系统状态故障");
                        System.exit(108);
                    }
                    if (queue.isEmpty())
                    {
                        closeChannel(new EndOfStreamException());
                        quitToIdle();
                    }
                    else
                    {
                        writeQueuedBuffer();
                    }
                }
            }
            case NOTICE_WORK ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, NOTICE_WORK, NOTICE_IDLE))
                {
                    if (queue.isEmpty())
                    {
                        ;
                    }
                    else
                    {
                        tryWork();
                    }
                }
            }
            default ->
            {
                log.error("系统状态故障");
                System.exit(109);
            }
        }
    }

    @Override
    public void completed(Integer result, ByteBuffer byteBuffer)
    {
        try
        {
            if (byteBuffer.hasRemaining())
            {
                socketChannel.write(byteBuffer, byteBuffer, this);
                return;
            }
            long currentSend = sendingData.getWritePosi();
            writeListener.partWriteFinish(currentSend);
            sendingData.clear();
            if (!queue.isEmpty())
            {
                writeQueuedBuffer();
                return;
            }
            for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
            {
                if (!queue.isEmpty())
                {
                    writeQueuedBuffer();
                    return;
                }
            }
            sendingData.free();
            sendingData = null;
            int now = state;
            switch (now)
            {
                case OPEN_WORK -> quitToIdle();
                case NOTICE_WORK ->
                {
                    if (queue.isEmpty())
                    {
                        closeChannel(new EndOfStreamException());
                        quitToIdle();
                    }
                    else
                    {
                        writeQueuedBuffer();
                    }
                }
            }
        }
        catch (Throwable e)
        {
            failed(e, byteBuffer);
        }
    }

    /**
     * 从MPSCQueue中取得IoBuffer，并且执行写操作
     */
    private void writeQueuedBuffer()
    {
        try
        {
            int      count = 0;
            IoBuffer buffer;
            while (count < maxWriteBytes && (buffer = queue.poll()) != null)
            {
                count += buffer.remainRead();
                if (sendingData == null)
                {
                    sendingData = buffer;
                }
                else
                {
                    sendingData.put(buffer);
                    buffer.free();
                }
            }
            ByteBuffer byteBuffer = sendingData.readableByteBuffer();
            socketChannel.write(byteBuffer, byteBuffer, this);
        }
        catch (Throwable e)
        {
            failed(e, null);
        }
    }

    @Override
    public void failed(Throwable e, ByteBuffer byteBuffer)
    {
        if (sendingData != null)
        {
            sendingData.free();
            sendingData = null;
        }
        writeListener.writeFailed(e);
        IoBuffer tmp;
        while ((tmp = queue.poll()) != null)
        {
            tmp.free();
        }
        closeChannel(e);
        quitToIdle();
    }
}
