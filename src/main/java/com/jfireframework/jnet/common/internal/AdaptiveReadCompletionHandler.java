package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.common.util.MathUtil;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdaptiveReadCompletionHandler extends BindDownAndUpStreamDataProcessor<IoBuffer> implements ReadCompletionHandler<IoBuffer>
{
    protected final AsynchronousSocketChannel socketChannel;
    protected final AioListener               aioListener;
    protected final BufferAllocator           allocator;
    protected final ReadEntry                 entry      = new ReadEntry();
    protected       ChannelContext            channelContext;
    static final    int[]                     sizeTable;
    private         int                       minIndex;
    private         int                       maxIndex;
    private         int                       index;
    private         boolean                   shouldDecr = false;

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

    public static void main(String[] args)
    {
        System.out.println(indexOf(16));
    }

    public AdaptiveReadCompletionHandler(ChannelConfig config, AsynchronousSocketChannel socketChannel)
    {
        this.aioListener = config.getAioListener();
        this.allocator = config.getAllocator();
        this.socketChannel = socketChannel;
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
                downStream.process(buffer);
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
        channelContext.close(e);
        entry.clean();
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public void process(IoBuffer data) throws Throwable
    {
        throw new UnsupportedOperationException("读完成器不应该执行该方法");
    }
}
