package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.ReadListener;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.exception.EndOfStreamException;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.MathUtil;
import lombok.Setter;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;

public class AdaptiveReadCompletionHandler implements CompletionHandler<Integer, AdaptiveReadCompletionHandler>
{
    public static final int[]                     sizeTable;

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

    protected final     AsynchronousSocketChannel socketChannel;
    protected final     BufferAllocator           allocator;
    private final       int                       minIndex;
    private final       int                       maxIndex;
    private final       int                       DECR_COUNT_MAX;
    private final InternalPipeline pipeline;
    @Setter
    private       ReadListener     readListener;
    private       int              index;
    private             int                       decrCount;
    private             IoBuffer                  ioBuffer;

    public AdaptiveReadCompletionHandler(InternalPipeline pipeline)
    {
        this.pipeline = pipeline;
        ChannelConfig config = pipeline.channelConfig();
        DECR_COUNT_MAX = config.getDecrCountMax();
        decrCount      = DECR_COUNT_MAX;
        socketChannel  = pipeline.socketChannel();
        allocator      = config.getAllocatorSupplier().get();
        minIndex       = indexOf(config.getMinReceiveSize());
        maxIndex       = indexOf(config.getMaxReceiveSize());
        index          = indexOf(config.getInitReceiveSize());
        index          = Math.max(index, minIndex);
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

    public void start()
    {
        ioBuffer = allocator.ioBuffer(sizeTable[index]);
        socketChannel.read(ioBuffer.writableByteBuffer(), this, this);
    }

    @Override
    public void completed(Integer read, AdaptiveReadCompletionHandler handler)
    {
        if (read == -1)
        {
            failed(new EndOfStreamException(), this);
            return;
        }
        int      except    = ioBuffer.capacity();
        IoBuffer thisRound = ioBuffer;
        ioBuffer = null;
        if (read != 0)
        {
            thisRound.addWritePosi(read);
            pipeline.fireRead(thisRound);
        }
        else
        {
            thisRound.free();
            System.err.println("读取到了0");
        }
        ioBuffer = nextReadBuffer(except, read);
        readListener.onRegister(this, pipeline);
    }

    public void registerRead()
    {
        try
        {
            socketChannel.read(ioBuffer.writableByteBuffer(), this, this);
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
        /**
         * 这些方法只能在这里被调用，因为InternalPipeline#fireReadFailed(java.lang.Throwable)方法可能在多个地方被调用，这样可能会违背当前的线程模型。
         * 在这个地方调用保证了这些方法的起点都是当前的线程.
         */
        pipeline.fireReadFailed(e);
        pipeline.shutdownInput();
    }
}
