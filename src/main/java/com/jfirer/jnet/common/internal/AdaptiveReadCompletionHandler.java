package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadCompletionHandler;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.MathUtil;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

public class AdaptiveReadCompletionHandler implements ReadCompletionHandler<IoBuffer>
{
    protected final AsynchronousSocketChannel socketChannel;
    protected final BufferAllocator           allocator;
    protected final ReadEntry                 entry      = new ReadEntry();
    protected       ChannelContext            channelContext;
    static final    int[]                     sizeTable;
    private         int                       minIndex;
    private         int                       maxIndex;
    private         int                       index;
    private         boolean                   shouldDecr = false;
    private         Pipeline                  pipeline;

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
        pipeline = channelContext.pipeline();
        socketChannel = channelContext.socketChannel();
        ChannelConfig config = channelContext.channelConfig();
        allocator = config.getAllocator();
        minIndex = indexOf(config.getMinReceiveSize());
        maxIndex = indexOf(config.getMaxReceiveSize());
        index = indexOf(config.getInitReceiveSize());
    }

    @Override
    public void start()
    {
        IoBuffer buffer = allocator.ioBuffer(sizeTable[index]);
        read(buffer, entry);
    }

    private void read(IoBuffer buffer, ReadEntry entry)
    {
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.writableByteBuffer());
        socketChannel.read(entry.getByteBuffer(), entry, this);
    }

    @Override
    public void completed(Integer length, ReadEntry entry)
    {
        int read = length;
        if (read == -1)
        {
            entry.clean();
            channelContext.close();
            return;
        }
        IoBuffer buffer = entry.getIoBuffer();
        int      except = buffer.capacity();
        if (read != 0)
        {
            buffer.addWritePosi(read);
            try
            {
                pipeline.fireRead(buffer);
            }
            catch (Throwable e)
            {
                failed(e, entry);
                return;
            }
        }
        try
        {
            IoBuffer nextReadBuffer = nextReadBuffer(except, read);
            read(nextReadBuffer, entry);
        }
        catch (Throwable e)
        {
            failed(e, entry);
            return;
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
    public void failed(Throwable e, ReadEntry entry)
    {
        try
        {
            pipeline.fireExceptionCatch(e);
            channelContext.close(e);
        }
        finally
        {
            entry.clean();
        }
    }
}
