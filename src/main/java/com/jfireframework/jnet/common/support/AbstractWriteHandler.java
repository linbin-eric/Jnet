package com.jfireframework.jnet.common.support;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.concurrent.MPSCLinkedQueue;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public abstract class AbstractWriteHandler implements WriteHandler
{
    protected static final Unsafe       unsafe         = ReflectUtil.getUnsafe();
    protected static final long         STATE_OFFSET   = UnsafeFieldAccess.getFieldOffset("state", AbstractWriteHandler.class);
    protected static final int          SPIN_THRESHOLD = 1 << 7;
    protected static final int          WORK           = 1;
    protected static final int          IDLE           = 2;
    // 终止状态。进入该状态后，不再继续使用
    protected static final int          TERMINATION    = 3;
    ////////////////////////////////////////////////////////////
    protected volatile int              state          = IDLE;
    protected MPSCLinkedQueue<IoBuffer> queue          = new MPSCLinkedQueue<>();
    protected WriteEntry                entry          = new WriteEntry();
    protected AsynchronousSocketChannel socketChannel;
    protected BufferAllocator           allocator;
    
    public AbstractWriteHandler(ChannelContext channelContext, BufferAllocator allocator)
    {
        socketChannel = channelContext.socketChannel();
        this.allocator = allocator;
    }
    
    @Override
    public void write(IoBuffer buf)
    {
        int now = state;
        if (now == TERMINATION)
        {
            throw new IllegalStateException("该通道已经处于关闭状态，无法执行写操作");
        }
        if (buf != null)
        {
            queue.offer(buf);
        }
        if (now == IDLE && changeToWork())
        {
            writeQueuedBuffer();
        }
    }
    
    /**
     * 从MPSCQueue中取得一个IoBuffer，并且执行写操作
     */
    abstract void writeQueuedBuffer();
    
    protected boolean changeToWork()
    {
        return unsafe.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
    }
    
    protected void rest()
    {
        state = IDLE;
        if (queue.isEmpty() == false)
        {
            writeQueuedBuffer();
        }
    }
    
}
