package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledBufferAllocator;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultReadCompletionHandler extends BindDownAndUpStreamDataProcessor<IoBuffer> implements ReadCompletionHandler<IoBuffer>
{
    public static        int                       initializeCapacity = PooledBufferAllocator.PAGESIZE;
    protected final      AsynchronousSocketChannel socketChannel;
    protected final      AioListener               aioListener;
    protected final      BufferAllocator           allocator;
    protected final      ReadEntry                 entry              = new ReadEntry();
    protected            ChannelContext            channelContext;

    public DefaultReadCompletionHandler(AioListener aioListener, BufferAllocator allocator, AsynchronousSocketChannel socketChannel)
    {
        this.aioListener = aioListener;
        this.allocator = allocator;
        this.socketChannel = socketChannel;
    }

    @Override
    public void start()
    {
        IoBuffer buffer = allocator.ioBuffer(initializeCapacity);
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.writableByteBuffer());
        socketChannel.read(entry.getByteBuffer(), entry, this);
    }

    @Override
    public void completed(Integer read, ReadEntry entry)
    {
        if (read == -1)
        {
            entry.clean();
            channelContext.close();
            return;
        }
        IoBuffer buffer = entry.getIoBuffer();
        buffer.addWritePosi(read);
        try
        {
            downStream.process(buffer);
            if (needCompact(buffer))
            {
                buffer.compact();
            }
            entry.setIoBuffer(buffer);
            entry.setByteBuffer(buffer.writableByteBuffer());
            socketChannel.read(entry.getByteBuffer(), entry, this);
        }
        catch (Throwable e)
        {
            failed(e, entry);
            return;
        }
    }

    private boolean needCompact(IoBuffer buffer)
    {
        return buffer.getReadPosi() > 1024 * 1024 && buffer.remainRead() < 1024;
    }

    @Override
    public void failed(Throwable exc, ReadEntry entry)
    {
        entry.clean();
        channelContext.close(exc);
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
