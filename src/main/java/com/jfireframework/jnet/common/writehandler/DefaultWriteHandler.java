package com.jfireframework.jnet.common.writehandler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.util.ByteBufFactory;

public class DefaultWriteHandler implements WriteHandler
{
    private final ByteBuf<?>[]              bufArray;
    private final static int                WORK             = 1;
    private final static int                IDLE             = 2;
    private final CpuCachePadingInt         status           = new CpuCachePadingInt(IDLE);
    private int                             currentSendCount = 0;
    private final int                       maxMerge;
    private final ChannelContext            channelContext;
    private final AsynchronousSocketChannel socketChannel;
    private final AioListener               aioListener;
    private final BufStorage                bufStorage;
    private static final int                SPIN_THRESHOLD   = 1 << 7;
    
    public DefaultWriteHandler(int maxMerge, AsynchronousSocketChannel socketChannel, AioListener channelListener, BufStorage bufStorage, ChannelContext serverChannelContext)
    {
        bufArray = new ByteBuf<?>[maxMerge];
        this.maxMerge = maxMerge;
        this.channelContext = serverChannelContext;
        this.socketChannel = socketChannel;
        this.aioListener = channelListener;
        this.bufStorage = bufStorage;
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
        writeNextBuf(buf);
    }
    
    private void writeNextBuf(ByteBuf<?> buf)
    {
        currentSendCount = bufStorage.batchNext(bufArray, maxMerge);
        if (currentSendCount != 0)
        {
            commitWrite(buf);
        }
        else
        {
            for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
            {
                if (bufStorage.isEmpty() == false)
                {
                    currentSendCount = bufStorage.batchNext(bufArray, maxMerge);
                    commitWrite(buf);
                    return;
                }
            }
            status.set(IDLE);
            if (bufStorage.isEmpty() == false)
            {
                registerWrite();
            }
            else if (buf != null)
            {
                ByteBufFactory.release(buf);
            }
        }
    }
    
    private void commitWrite(ByteBuf<?> buf)
    {
        int needSize = 0;
        for (int i = 0; i < currentSendCount; i++)
        {
            needSize += bufArray[i].remainRead();
        }
        if (buf == null)
        {
            buf = ByteBufFactory.allocate(needSize);
        }
        for (int i = 0; i < currentSendCount; i++)
        {
            buf.put(bufArray[i]);
            ByteBufFactory.release(bufArray[i]);
            bufArray[i] = null;
        }
        socketChannel.write(buf.nioBuffer(), buf, this);
    }
    
    @Override
    public void failed(Throwable exc, ByteBuf<?> buf)
    {
        ByteBufFactory.release(buf);
        try
        {
            aioListener.catchException(exc, channelContext);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (channelContext.isOpen() == false)
            {
                do
                {
                    buf = bufStorage.next();
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
    }
    
    public void registerWrite()
    {
        int now = status.value();
        if (now == IDLE && status.compareAndSwap(IDLE, WORK))
        {
            writeNextBuf(null);
        }
    }
}
