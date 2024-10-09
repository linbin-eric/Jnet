package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadCompletionHandler;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.exception.EndOfStreamException;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.MathUtil;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdaptiveReadCompletionHandler implements ReadCompletionHandler
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
    protected           ChannelContext            channelContext;
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

    public AdaptiveReadCompletionHandler(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
        msOfReadTimeout     = channelContext.channelConfig().getMsOfReadTimeout();
        DECR_COUNT_MAX      = channelContext.channelConfig().getDecrCountMax();
        decrCount           = DECR_COUNT_MAX;
        pipeline            = (InternalPipeline) channelContext.pipeline();
        socketChannel       = channelContext.socketChannel();
        ChannelConfig config = channelContext.channelConfig();
        allocator = config.getAllocator();
        minIndex  = indexOf(config.getMinReceiveSize());
        maxIndex  = indexOf(config.getMaxReceiveSize());
        index     = indexOf(config.getInitReceiveSize());
        index     = Math.max(index, minIndex);
    }

    @Override
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
    public void completed(Integer read, ReadCompletionHandler handler)
    {
        if (read == -1)
        {
            ioBuffer.free();
            EndOfStreamException exception = new EndOfStreamException();
            channelContext.close(exception);
            pipelineClose(exception);
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
    public void failed(Throwable e, ReadCompletionHandler handler)
    {
        if (ioBuffer != null)
        {
            ioBuffer.free();
        }
        channelContext.close(e);
        pipelineClose(e);
    }

    private void pipelineClose(Throwable e)
    {
        Pipeline.invokeMethodIgnoreException(pipeline::fireReadClose);
        Pipeline.invokeMethodIgnoreException(pipeline::fireWriteClose);
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireExceptionCatch(e));
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireChannelClose(e));
    }
}
