package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.util.UNSAFE;
import org.jctools.queues.MpscLinkedQueue;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * 支持并发的 WriteProcessorNode 实现，作为写处理链的头节点。
 * 使用 MpscLinkedQueue 队列和虚拟线程实现并发安全的写请求处理。
 */
public class ConcurrentWriteProcessorNode implements WriteProcessorNode, Runnable
{
    private static final int                     IDLE         = -1;
    private static final int                     WORK         = 1;
    private static final long                    STATE_OFFSET = UNSAFE.getFieldOffset("state", ConcurrentWriteProcessorNode.class);
    private final        MpscLinkedQueue<Object> queue        = new MpscLinkedQueue<>();
    private volatile     int                     state        = IDLE;
    private final        Thread                  thread;
    private              WriteProcessorNode      next;
    private final        Pipeline                pipeline;

    public ConcurrentWriteProcessorNode(Pipeline pipeline)
    {
        this.pipeline = pipeline;
        Consumer<Throwable> uncaughtExceptionHandler = pipeline.channelConfig().getJvmExistHandler();
        this.thread = Thread.ofVirtual().uncaughtExceptionHandler((t, e) -> uncaughtExceptionHandler.accept(e)).unstarted(this);
        this.thread.start();
    }

    @Override
    public void fireWrite(Object data)
    {
        queue.offer(data);
        int t_state = this.state;
        if (t_state == IDLE)
        {
            if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
            {
                LockSupport.unpark(thread);
//                Thread.startVirtualThread(this);
            }
        }
    }

    @Override
    public void fireWriteFailed(Throwable e)
    {
        queue.offer(e);
        int t_state = this.state;
        if (t_state == IDLE)
        {
            if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
            {
//                Thread.startVirtualThread(this);
                LockSupport.unpark(thread);
            }
        }
    }

    @Override
    public void run()
    {
        do
        {
            Object avail = queue.poll();
            if (avail != null)
            {
                if (avail instanceof Throwable)
                {
                    next.fireWriteFailed((Throwable) avail);
                }
                else
                {
                    next.fireWrite(avail);
                }
            }
            else
            {
                state = IDLE;
                if (!queue.isEmpty())
                {
//                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
//                    {
//                        ;
//                    }
//                    else
//                    {
//                        return;
//                    }
                    UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK);
                }
                else
                {
                    LockSupport.park(thread);
//                    return;
                }
            }
        } while (true);
    }

    @Override
    public WriteProcessorNode getNext()
    {
        return next;
    }

    @Override
    public void setNext(WriteProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
