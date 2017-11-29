package com.jfireframework.jnet.common.support;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.util.ByteBufFactory;

public class WriteHandler implements CompletionHandler<Integer, ByteBuf<?>>
{
    private static final int                SPIN_THRESHOLD   = 1 << 7;
    private final static int                WORK             = 1;
    private final static int                IDLE             = 2;
    // 终止状态。进入该状态后，不再继续使用
    private final static int                TERMINATION      = 3;
    private final ByteBuf<?>                outCachedBuf     = DirectByteBuf.allocate(1024);
    private final ByteBuf<?>[]              bufArray;
    private final CpuCachePadingInt         status           = new CpuCachePadingInt(IDLE);
    private final AsynchronousSocketChannel socketChannel;
    private final AioListener               aioListener;
    // private final SendBufStorage bufStorage = new MpscBufStorage();
    private MPSCQueue<ByteBuf<?>>           storage          = new MPSCQueue<>();
    private final ChannelContext            channelContext;
    private int                             currentSendCount = 0;
    
    public WriteHandler(AioListener aioListener, ChannelContext channelContext, int maxMerge)
    {
        this.aioListener = aioListener;
        this.channelContext = channelContext;
        this.socketChannel = channelContext.socketChannel();
        bufArray = new ByteBuf<?>[maxMerge];
    }
    
    @Override
    public void completed(Integer result, ByteBuf<?> buf)
    {
        ByteBuffer buffer = buf.cachedNioBuffer();
        if (buffer.hasRemaining())
        {
            socketChannel.write(buffer, buf, this);
            return;
        }
        buf.clear();
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
            ByteBufFactory.release(bufArray[i]);
            bufArray[i] = null;
        }
        socketChannel.write(outCachedBuf.nioBuffer(), outCachedBuf, this);
    }
    
    @Override
    public void failed(Throwable exc, ByteBuf<?> buf)
    {
        status.set(TERMINATION);
        ByteBufFactory.release(buf);
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
                    ByteBufFactory.release(buf);
                }
                else
                {
                    break;
                }
            } while (true);
        }
    }
    
    public void write(ByteBuf<?> buf)
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
