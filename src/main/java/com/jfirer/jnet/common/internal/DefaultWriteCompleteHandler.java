package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.WriteCompletionHandler;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.SpscLinkedQueue;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;

public class DefaultWriteCompleteHandler implements WriteCompletionHandler
{
    protected static final long                      STATE_OFFSET   = UNSAFE.getFieldOffset("state", DefaultWriteCompleteHandler.class);
    protected static final int                       SPIN_THRESHOLD = 16;
    protected static final int                       IDLE           = 1;
    protected static final int                       WORK           = 2;
    protected final        AsynchronousSocketChannel socketChannel;
    protected final        ChannelContext            channelContext;
    protected final        BufferAllocator           allocator;
    protected final        int                       maxWriteBytes;
    // 终止状态。进入该状态后，不再继续使用
    ////////////////////////////////////////////////////////////
    protected volatile     int                       state          = IDLE;
    //注意，不能使用JcTools下面的SpscQueue，其实现会出现当queue.isEmpty()==false时，queue.poll()返回null，导致程序异常
    //MpscQueue则是可以的。JDK的并发queue也是可以的
    protected              Queue<IoBuffer>           queue          = new SpscLinkedQueue<>();
    private                IoBuffer                  sendingData;

    public DefaultWriteCompleteHandler(ChannelContext channelContext)
    {
        this.socketChannel = channelContext.socketChannel();
        ChannelConfig channelConfig = channelContext.channelConfig();
        this.allocator = channelConfig.getAllocator();
        this.maxWriteBytes = Math.max(1, channelConfig.getMaxBatchWrite());
        this.channelContext = channelContext;
    }

    @Override
    public void write(IoBuffer buffer)
    {
        System.out.println(System.identityHashCode(this) + "写入" + buffer.remainRead());
        if (buffer == null)
        {
            throw new NullPointerException();
        }
        queue.offer(buffer);
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
        else
        {
            System.out.println(System.identityHashCode(this) + "竞争失败");
        }
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
    public void completed(Integer result, ByteBuffer byteBuffer)
    {
        System.out.println(System.identityHashCode(this) + "写动作完毕");
        try
        {

            if (byteBuffer.hasRemaining())
            {
                System.out.println(System.identityHashCode(this) + "数据没完整写出");
                socketChannel.write(byteBuffer, byteBuffer, this);
                return;
            }
            sendingData.free();
            sendingData = null;
            if (queue.isEmpty() == false)
            {
                System.out.println(System.identityHashCode(this) + "队列有数据，准备写出");
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
            int      count        = 0;
            IoBuffer buffer;
            IoBuffer accumulation = null;
            while (count < maxWriteBytes && (buffer = queue.poll()) != null)
            {
                count += buffer.remainRead();
                if (accumulation == null)
                {
                    accumulation = buffer;
                }
                else
                {
                    accumulation.put(buffer);
                    buffer.free();
                }
            }
            sendingData = accumulation;
            ByteBuffer byteBuffer = sendingData.readableByteBuffer();
            System.out.println(System.identityHashCode(this) + "发送" + sendingData.remainRead());
            socketChannel.write(byteBuffer, byteBuffer, this);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            Pipeline.invokeMethodIgnoreException(() -> ((InternalPipeline) channelContext.pipeline()).fireExceptionCatch(e));
        }
    }

    @Override
    public void failed(Throwable e, ByteBuffer byteBuffer)
    {
        if (sendingData != null)
        {
            sendingData.free();
        }
        prepareTermination();
        InternalPipeline pipeline = (InternalPipeline) channelContext.pipeline();
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireExceptionCatch(e));
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireWriteClose());
        channelContext.close(e);
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
