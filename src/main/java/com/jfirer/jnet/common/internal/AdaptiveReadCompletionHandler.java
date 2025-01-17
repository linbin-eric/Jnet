package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.exception.EndOfStreamException;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.MathUtil;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdaptiveReadCompletionHandler implements CompletionHandler<Integer, AdaptiveReadCompletionHandler>
{
    public static final int[]                     sizeTable;
    protected final     AsynchronousSocketChannel socketChannel;
    protected final     BufferAllocator           allocator;
    private final       int                       minIndex;
    private final       int                       maxIndex;
    private final       int                       DECR_COUNT_MAX;
    private final       InternalPipeline          pipeline;
    private final       long                      msOfReadTimeout;
    private             int                       index;
    private             int                       decrCount;
    private             IoBuffer                  ioBuffer;

    static
    {
        List<Integer> list = new ArrayList<>();
        for (int i = 16; i < 512; i += 16)
        {
            list.add(i);
        }
        for (int i = 512; i > 0; i <<= 1)
        {
            list.add(i);
        }
        sizeTable = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            sizeTable[i] = list.get(i);
        }
    }

    static int indexOf(int num)
    {
        if (num == 0)
        {
            return 0;
        }
        if (num < 512)
        {
            int base = (num >> 4) - 1;
            if ((num & 0x0f) != 0)
            {
                base += 1;
            }
            return base;
        }
        else
        {
            return MathUtil.log2(MathUtil.normalizeSize(num)) + 22;
        }
    }

    public AdaptiveReadCompletionHandler(InternalPipeline pipeline)
    {
        this.pipeline = pipeline;
        ChannelConfig config = pipeline.channelConfig();
        msOfReadTimeout = config.getMsOfReadTimeout();
        DECR_COUNT_MAX  = config.getDecrCountMax();
        decrCount       = DECR_COUNT_MAX;
        socketChannel   = pipeline.socketChannel();
        allocator       = config.getAllocator();
        minIndex        = indexOf(config.getMinReceiveSize());
        maxIndex        = indexOf(config.getMaxReceiveSize());
        index           = indexOf(config.getInitReceiveSize());
        index           = Math.max(index, minIndex);
    }

    public void start()
    {
        ioBuffer = allocator.ioBuffer(sizeTable[index]);
        if (msOfReadTimeout == -1)
        {
            socketChannel.read(ioBuffer.writableByteBuffer(), this, this);
        }
        else
        {
            socketChannel.read(ioBuffer.writableByteBuffer(), msOfReadTimeout, TimeUnit.MILLISECONDS, this, this);
        }
    }

    @Override
    public void completed(Integer read, AdaptiveReadCompletionHandler handler)
    {
        if (read == -1)
        {
            failed(new EndOfStreamException(), this);
            return;
        }
        int except = ioBuffer.capacity();
        try
        {
            if (read != 0)
            {
                ioBuffer.addWritePosi(read);
                pipeline.fireRead(ioBuffer);
            }
            else
            {
                ioBuffer.free();
                System.err.println("读取到了0");
            }
            ioBuffer = nextReadBuffer(except, read);
            socketChannel.read(ioBuffer.writableByteBuffer(), msOfReadTimeout, TimeUnit.MILLISECONDS, this, this);
        }
        catch (Throwable e)
        {
            failed(e, this);
        }
    }

    IoBuffer nextReadBuffer(int expect, int real)
    {
        if (expect == real)
        {
            if (index != maxIndex)
            {
                index += 1;
            }
            decrCount = DECR_COUNT_MAX;
        }
        else
        {
            int needIndex = indexOf(real);
            if (index > needIndex && index != minIndex)
            {
                if (--decrCount < 0)
                {
                    index -= 1;
                    decrCount = DECR_COUNT_MAX;
                }
            }
        }
        return allocator.ioBuffer(sizeTable[index]);
    }

    @Override
    public void failed(Throwable e, AdaptiveReadCompletionHandler handler)
    {
        if (ioBuffer != null)
        {
            ioBuffer.free();
        }
        pipeline.close(e);
        /**
         * 这些方法只能在这里被调用，因为Pipeline#close(java.lang.Throwable)方法可能在多个地方被调用，这样可能会违背当前的线程模型。
         * 在这个地方调用保证了这些方法的起点都是当前的线程，并且是只会被执行一次。
         */
        fireMethodIgnoreException(pipeline::fireReadClose);
        fireMethodIgnoreException(pipeline::fireWriteClose);
        fireMethodIgnoreException(() -> pipeline.fireExceptionCatch(e));
        fireMethodIgnoreException(() -> pipeline.fireChannelClose(e));
    }

    void fireMethodIgnoreException(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable e)
        {
            ;
        }
    }
}
