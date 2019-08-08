package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultReadCompletionHandler extends BindDownAndUpStreamDataProcessor<IoBuffer> implements ReadCompletionHandler<IoBuffer>
{
    protected final      AsynchronousSocketChannel socketChannel;
    protected final      AioListener               aioListener;
    protected final      BufferAllocator           allocator;
    protected final      ReadEntry                 entry = new ReadEntry();
    protected            ChannelContext            channelContext;
    private static final int                       IDLE  = 0;
    private static final int                       WORK  = 1;
    AtomicInteger state = new AtomicInteger(IDLE);

    public DefaultReadCompletionHandler(AioListener aioListener, BufferAllocator allocator, AsynchronousSocketChannel socketChannel)
    {
        this.aioListener = aioListener;
        this.allocator = allocator;
        this.socketChannel = socketChannel;
    }

    @Override
    public void start()
    {
        IoBuffer buffer = allocator.ioBuffer(128);
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.writableByteBuffer());
        state.set(WORK);
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
        }
        catch (Throwable e)
        {
            failed(e, entry);
            return;
        }
        if (needCompact(buffer))
        {
            buffer.compact();
        }
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.writableByteBuffer());
        socketChannel.read(entry.getByteBuffer(), entry, this);
    }

    private boolean needCompact(IoBuffer buffer)
    {
        return buffer.getReadPosi() > 1024 * 1024 && buffer.remainRead() < 1024;
    }

    @Override
    public void failed(Throwable exc, ReadEntry entry)
    {
        entry.getIoBuffer().free();
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
