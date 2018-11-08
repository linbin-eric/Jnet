package com.jfireframework.jnet.common.internal;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultReadCompletionHandler extends BindDownAndUpStreamDataProcessor<IoBuffer> implements ReadCompletionHandler<IoBuffer>
{
    protected final AsynchronousSocketChannel socketChannel;
    protected final AioListener               aioListener;
    protected final BufferAllocator           allocator;
    protected final ReadEntry                 entry = new ReadEntry();
    protected       ChannelContext            channelContext;
    AtomicInteger ignoreNotify = new AtomicInteger(0);

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
        socketChannel.read(entry.getByteBuffer(), entry, this);
    }

    @Override
    public void completed(Integer read, ReadEntry entry)
    {
        if (read == -1)
        {
            try
            {
                socketChannel.close();
            } catch (Throwable e)
            {
                catchException(e);
                ReflectUtil.throwException(e);
            } finally
            {
                entry.getIoBuffer().free();
            }
            return;
        }
        IoBuffer buffer = entry.getIoBuffer();
        buffer.addWritePosi(read);
        try
        {
            if (downStream.process(buffer) == false)
            {
                ignoreNotify.set(1);
                boolean continueRead = false;
                while (downStream.canAccept() && (ignoreNotify.get()) == 1)
                {
                    if (ignoreNotify.compareAndSet(1, 0))
                    {
                        if (downStream.process(buffer))
                        {
                            continueRead = true;
                            break;
                        }
                        else
                        {
                            ignoreNotify.set(1);
                        }
                    }
                }
                if (continueRead == false)
                {
//                    System.out.println(Thread.currentThread().getName()+"下游不可用，中断读取");
                    return;
                }
            }
        } catch (Throwable e)
        {
            catchException(e);
            buffer.free();
            try
            {
                socketChannel.close();
            } catch (IOException e1)
            {
                ;
            }
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

    private void catchException(Throwable e)
    {
        if (aioListener != null)
        {
            aioListener.catchException(e, socketChannel);
        }
    }

    @Override
    public void failed(Throwable exc, ReadEntry entry)
    {
        entry.getIoBuffer().free();
        catchException(exc);
    }

    @Override
    public void bind(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public boolean process(IoBuffer data) throws Throwable
    {
        throw new UnsupportedOperationException("读完成器不应该执行该方法");
    }

    @Override
    public void notifyedWriteAvailable()
    {
        int now = 0;
        while (downStream.canAccept() && (now= ignoreNotify.get()) == 1)
        {
            if (ignoreNotify.compareAndSet(1, 0))
            {
                IoBuffer buffer = entry.getIoBuffer();
                try
                {
//                    System.out.println(Thread.currentThread().getName()+"准备处理读完成器的剩余数据");
                    if (downStream.process(buffer))
                    {
//                        System.out.println(Thread.currentThread().getName()+"处理剩余数据");
                        if (needCompact(buffer))
                        {
                            buffer.compact();
                        }
//                        System.out.println(Thread.currentThread().getName()+"恢复读取");
                        entry.setByteBuffer(buffer.writableByteBuffer());
                        socketChannel.read(entry.getByteBuffer(), entry, this);
                        return;
                    }
                    else
                    {
//                        System.out.println(Thread.currentThread().getName()+"剩余数据处理导致下游阻塞，再次等待唤醒");
                        ignoreNotify.set(1);
                    }
                } catch (Throwable e)
                {
                    e.printStackTrace();
                    catchException(e);
                    buffer.free();
                    try
                    {
                        socketChannel.close();
                    } catch (IOException e1)
                    {
                        ;
                    }
                    return;
                }
            }
        }
//        System.out.println(Thread.currentThread().getName()+"不满足唤醒条件，当前忽略状态："+now+","+downStream.canAccept());
    }
}
