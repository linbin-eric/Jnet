package com.jfireframework.jnet.common.support;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCLinkedQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.util.Allocator;

public class DefaultWriteHandler implements WriteHandler
{
    private static final int                SPIN_THRESHOLD   = 1 << 7;
    private final static int                WORK             = 1;
    private final static int                IDLE             = 2;
    // 终止状态。进入该状态后，不再继续使用
    private final static int                TERMINATION      = 3;
    private final IoBuffer[]                bufArray;
    private final CpuCachePadingInt         status           = new CpuCachePadingInt(IDLE);
    private final AsynchronousSocketChannel socketChannel;
    private final AioListener               aioListener;
    private MPSCLinkedQueue<IoBuffer>       storage          = new MPSCLinkedQueue<>();
    private final ChannelContext            channelContext;
    private int                             currentSendCount = 0;
    private final BufferAllocator           allocator;
    
    public DefaultWriteHandler(AioListener aioListener, ChannelContext channelContext, int maxMerge, BufferAllocator allocator)
    {
        this.allocator = allocator;
        this.aioListener = aioListener;
        this.channelContext = channelContext;
        this.socketChannel = channelContext.socketChannel();
        bufArray = new IoBuffer[maxMerge];
    }
    
    @Override
    public void completed(Integer result, WriteEntry entry)
    {
        ByteBuffer buffer = entry.getByteBuffer();
        if (buffer.hasRemaining())
        {
            socketChannel.write(buffer, entry, this);
            return;
        }
        e
        aioListener.afterWrited(channelContext, currentSendCount);
        writeNextBuf();
    }
    
    private void writeNextBuf()
    {
        currentSendCount = storage.drain(bufArray, bufArray.length);
        if (currentSendCount != 0)
        {
            commitWrite();
        }
        else
        {
            for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
            {
                if (storage.isEmpty() == false)
                {
                    currentSendCount = storage.drain(bufArray, bufArray.length);
                    commitWrite();
                    return;
                }
            }
            status.set(IDLE);
            if (storage.isEmpty() == false)
            {
                write(null);
            }
        }
    }
    
    private void commitWrite()
    {
        for (int i = 0; i < currentSendCount; i++)
        {
            outCachedBuf.put(bufArray[i]);
            Allocator.release(bufArray[i]);
            bufArray[i] = null;
        }
        socketChannel.write(outCachedBuf.byteBuffer(), outCachedBuf, this);
    }
    
    @Override
    public void failed(Throwable exc, PooledIoBuffer buf)
    {
        status.set(TERMINATION);
        buf.release();
        try
        {
            socketChannel.close();
            aioListener.catchException(exc, channelContext);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            do
            {
                buf = storage.poll();
                if (buf != null)
                {
                    buf.release();
                }
                else
                {
                    break;
                }
            } while (true);
        }
    }
    
    public void write(PooledIoBuffer buf)
    {
        if (buf != null)
        {
            storage.offer(buf);
        }
        int now = status.value();
        if (now == IDLE && status.compareAndSwap(IDLE, WORK))
        {
            writeNextBuf();
        }
    }
}
