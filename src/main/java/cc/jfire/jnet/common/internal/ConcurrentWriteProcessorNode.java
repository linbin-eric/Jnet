package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.util.UNSAFE;
import org.jctools.queues.MpscLinkedQueue;

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
    private              WriteProcessorNode      next;
    private final        Pipeline                pipeline;

    public ConcurrentWriteProcessorNode(Pipeline pipeline)
    {
        this.pipeline = pipeline;
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
                Thread.startVirtualThread(this);
            }
        }
    }

    @Override
    public void fireQueueEmpty()
    {
        throw new UnsupportedOperationException();
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
                Thread.startVirtualThread(this);
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
                if (avail instanceof Throwable e)
                {
                    next.fireWriteFailed(e);
                }
                else
                {
                    next.fireWrite(avail);
                }
            }
            else
            {
                boolean needIdle = true;
                for (int spin = 0; spin < 32; spin++)
                {
                    if (queue.isEmpty())
                    {
                        Thread.onSpinWait();
                    }
                    else
                    {
                        needIdle = false;
                    }
                }
                if (needIdle == false)
                {
                    continue;
                }
                state = IDLE;
                if (!queue.isEmpty() && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
                {
                    ;
                }
                else
                {
                    return;
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
