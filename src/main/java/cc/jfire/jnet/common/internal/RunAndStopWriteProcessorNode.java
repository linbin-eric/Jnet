package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.util.UNSAFE;
import lombok.Getter;
import lombok.Setter;
import org.jctools.queues.MpscLinkedQueue;

import java.util.concurrent.locks.LockSupport;

public class RunAndStopWriteProcessorNode implements WriteProcessorNode, Runnable
{
    private final        Pipeline                pipeline;
    @Getter
    @Setter
    private              WriteProcessorNode      next;
    private static final int                     IDLE_OPEN     = 1;
    private static final int                     WORK_OPEN     = 2;
    private static final int                     WORK_CLOSE    = 4;
    private static final int                     TERMINATION   = 10;
    private static final long                    STATE_OFFSET  = UNSAFE.getFieldOffset("state", RunAndStopWriteProcessorNode.class);
    private final        MpscLinkedQueue<Object> queue         = new MpscLinkedQueue<>();
    private volatile     int                     state         = WORK_OPEN;
    private              Thread                  thread;
    private              Throwable               e;
    private static final int                     UN_FIRE       = 0;
    private static final int                     FIRED         = 1;
    private volatile     int                     fireE         = UN_FIRE;
    private static final long                    FIRE_E_OFFSET = UNSAFE.getFieldOffset("fireE", RunAndStopWriteProcessorNode.class);

    public RunAndStopWriteProcessorNode(Pipeline pipeline)
    {
        this.pipeline = pipeline;
        thread        = Thread.startVirtualThread(this);
    }

    @Override
    public void fireWrite(Object data)
    {
        queue.offer(data);
        int t_state = state;
        switch (t_state)
        {
            case IDLE_OPEN ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE_OPEN, WORK_OPEN))
                {
                    LockSupport.unpark(thread);
                }
                else
                {
                    //没成功意味着其他线程成功了，忽略
                }
            }
            case WORK_OPEN,WORK_CLOSE ->
            {
                //已经在工作，忽略
            }
            case TERMINATION ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, TERMINATION, WORK_CLOSE))
                {
                    Thread.startVirtualThread(this);
                }
                else
                {
                    //没成功意味着其他线程成功了，忽略
                }
            }
        }
    }

    @Override
    public void fireChannelClosed(Throwable e)
    {
        this.e = e;
        int t_state = state;
        switch (t_state)
        {
            case IDLE_OPEN ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE_OPEN, WORK_CLOSE))
                {
                    LockSupport.unpark(thread);
                    return;
                }
            }
            case WORK_OPEN ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_OPEN, WORK_CLOSE))
                {
                    return;
                }
            }
            case WORK_CLOSE, TERMINATION -> throw new IllegalStateException();
        }
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }

    @Override
    public void run()
    {
        while (true)
        {
            Object poll = queue.poll();
            if (poll != null)
            {
                next.fireWrite(poll);
            }
            else
            {
                int t_state = state;
                if (t_state == WORK_OPEN)
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_OPEN, IDLE_OPEN))
                    {
                        if (queue.isEmpty())
                        {
                            do
                            {
                                LockSupport.park();
                                t_state = state;
                            } while (t_state == IDLE_OPEN);
                        }
                        else
                        {
                            t_state = state;
                            if (t_state == IDLE_OPEN)
                            {
                                UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE_OPEN, WORK_OPEN);
                            }
                        }
                    }
                    else
                    {
                        //如果 cas 失败，状态只会是 work_close。
                        t_state = state;
                        if (t_state != WORK_CLOSE)
                        {
                            throw new IllegalStateException();
                        }
                    }
                }
                else
                {
                    state = TERMINATION;
                    if (fireE == UN_FIRE && UNSAFE.compareAndSwapInt(this, FIRE_E_OFFSET, UN_FIRE, FIRED))
                    {
                        next.fireChannelClosed(e);
                    }
                    if (!queue.isEmpty())
                    {
                        if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, TERMINATION, WORK_CLOSE))
                        {
                            Thread.startVirtualThread(this);
                        }
                    }
                    return;
                }
            }
        }
    }
}
