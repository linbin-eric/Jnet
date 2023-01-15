package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadCompletionHandler;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.MathUtil;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

public class AdaptiveReadCompletionHandler implements ReadCompletionHandler<IoBuffer>
{
    protected final AsynchronousSocketChannel socketChannel;
    protected final BufferAllocator           allocator;
    protected       ChannelContext            channelContext;
    static final    int[]                     sizeTable;
    private         int                       minIndex;
    private         int                       maxIndex;
    private         int                       index;
    private         boolean                   shouldDecr = false;
    private         InternalPipeline          pipeline;
    private         IoBuffer                  ioBuffer;
    static
    {
        List<Integer> list = new ArrayList<>();
        for (int i = 16; i < 512; i += 16)
        {
            list.add(i);
        }
        for (int i = 512; i < Integer.MAX_VALUE && i > 0; i <<= 1)
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
            int i = MathUtil.log2(MathUtil.normalizeSize(num)) + 22;
            return i;
        }
    }

    public AdaptiveReadCompletionHandler(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
        pipeline = (InternalPipeline) channelContext.pipeline();
        socketChannel = channelContext.socketChannel();
        ChannelConfig config = channelContext.channelConfig();
        allocator = config.getAllocator();
        minIndex = indexOf(config.getMinReceiveSize());
        maxIndex = indexOf(config.getMaxReceiveSize());
        index = indexOf(config.getInitReceiveSize());
        index = index < minIndex ? minIndex : index;
    }

    @Override
    public void start()
    {
        ioBuffer = allocator.ioBuffer(sizeTable[index]);
        socketChannel.read(ioBuffer.writableByteBuffer(), this, this);
    }

    @Override
    public void completed(Integer read, ReadCompletionHandler handler)
    {
        if (read == -1)
        {
            ioBuffer.free();
            Pipeline.invokeMethodIgnoreException(() -> pipeline.fireReadClose());
            channelContext.close();
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
            ioBuffer = nextReadBuffer(except, read);
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
            shouldDecr = false;
        }
        else
        {
            int needIndex = indexOf(real);
            if (index > needIndex && index != minIndex)
            {
                if (shouldDecr == false)
                {
                    shouldDecr = true;
                }
                else
                {
                    index -= 1;
                    shouldDecr = false;
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
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireExceptionCatch(e));
        Pipeline.invokeMethodIgnoreException(() -> pipeline.fireReadClose());
        channelContext.close(e);
    }
}
