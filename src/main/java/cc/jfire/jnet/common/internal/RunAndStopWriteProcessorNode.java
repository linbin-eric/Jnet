package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteCompletionHandler;
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
    private static final int                     WORK_IDLE        = 0b000;
    private static final int                     WORK_BUSY        = 0b001;
    private static final int                     SHUTDOWN         = 0b010;
    private static final int                     TERMINATION_IDLE = 0b100;
    private static final int                     TERMINATION_BUSY = 0b101;
    private static final long                    STATE_OFFSET     = UNSAFE.getFieldOffset("state", RunAndStopWriteProcessorNode.class);
    private final        MpscLinkedQueue<Object> queue            = new MpscLinkedQueue<>();
    private volatile     int                     state            = WORK_IDLE;
    private              Thread                  thread;
    private              Throwable               e;
    private static final int                     UN_FIRE          = 0;
    private static final int                     FIRED            = 1;
    private volatile     int                     fireE            = UN_FIRE;
    private static final long                    FIRE_E_OFFSET    = UNSAFE.getFieldOffset("fireE", RunAndStopWriteProcessorNode.class);
    @Setter
    private              WriteCompletionHandler  writeCompletionHandler;

    public RunAndStopWriteProcessorNode(Pipeline pipeline)
    {
        this.pipeline = pipeline;
    }

    @Override
    public void fireWrite(Object data)
    {
        queue.offer(data);
        int t_state = state;
        switch (t_state)
        {
            case WORK_IDLE ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_IDLE, WORK_BUSY))
                {
                    LockSupport.unpark(thread);
                }
                else
                {
                    //没成功意味着其他线程成功了，忽略
                }
            }
            case WORK_BUSY, SHUTDOWN, TERMINATION_BUSY ->
            {
                //已经在工作，忽略
            }
            case TERMINATION_IDLE ->
            {
                if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, TERMINATION_IDLE, TERMINATION_BUSY))
                {
                    start();
                }
                else
                {
                    //没成功意味着其他线程成功了，忽略
                }
            }
        }
    }

    @Override
    public void fireShutdown()
    {
        while (true)
        {
            int t_state = state;
            switch (t_state)
            {
                case WORK_IDLE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_IDLE, SHUTDOWN))
                    {
                        LockSupport.unpark(thread);
                        return;
                    }
                }
                case WORK_BUSY -> UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_BUSY, SHUTDOWN);
                case TERMINATION_BUSY, TERMINATION_IDLE, SHUTDOWN ->
                {
                    return;
                }
            }
        }
    }

    @Override
    public void fireChannelClosed(Throwable e)
    {
        this.e = e;
        while (true)
        {
            int t_state = state;
            switch (t_state)
            {
                case WORK_IDLE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_IDLE, TERMINATION_BUSY))
                    {
                        LockSupport.unpark(thread);
                        return;
                    }
                }
                case WORK_BUSY -> UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_BUSY, TERMINATION_BUSY);
                case SHUTDOWN -> UNSAFE.compareAndSwapInt(this, STATE_OFFSET, SHUTDOWN, TERMINATION_BUSY);
                case TERMINATION_BUSY, TERMINATION_IDLE ->
                {
                    return;
                }
            }
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
                switch (t_state)
                {
                    case WORK_BUSY ->
                    {
                        if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_BUSY, WORK_IDLE))
                        {
                            if (queue.isEmpty())
                            {
                                do
                                {
                                    LockSupport.park();
                                    t_state = state;
                                } while (t_state == WORK_IDLE);
                            }
                            else
                            {
                                t_state = state;
                                if (t_state == WORK_IDLE)
                                {
                                    //不需要关心结果了，成功了需要继续循环；失败了，意味着别的线程将当前唤醒了，还是需要继续循环
                                    UNSAFE.compareAndSwapInt(this, STATE_OFFSET, WORK_IDLE, WORK_BUSY);
                                }
                            }
                        }
                        else
                        {
                            //如果 cas 失败，可能性有：shutdown，termination
                            t_state = state;
                            if (t_state != SHUTDOWN && t_state != TERMINATION_IDLE && t_state != TERMINATION_BUSY)
                            {
                                System.exit(2);
                            }
                        }
                    }
                    case SHUTDOWN ->
                    {
                        next.fireShutdown();
                        writeCompletionHandler.noticeClose();
                        UNSAFE.compareAndSwapInt(this, STATE_OFFSET, SHUTDOWN, WORK_BUSY);
                    }
                    case TERMINATION_BUSY ->
                    {
                        if (fireE == UN_FIRE && UNSAFE.compareAndSwapInt(this, FIRE_E_OFFSET, UN_FIRE, FIRED))
                        {
                            next.fireChannelClosed(e);
                        }
                        state = TERMINATION_IDLE;
                        if (!queue.isEmpty())
                        {
                            if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, TERMINATION_IDLE, TERMINATION_BUSY))
                            {
                                start();
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    public void start()
    {
        thread = Thread.ofVirtual().unstarted(this);
        thread.start();
    }
}
