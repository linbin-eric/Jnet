package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * 注意，一个JnetWorker会被分配给不同的通道，这意味着有多个生产者
 */
public class JnetWorkerImpl extends Thread implements JnetWorker
{
    private static final int             IDLE     = -1;
    private static final int             WORK     = 1;
    private              Queue<Runnable> queue    = new LinkedTransferQueue<>();
    private volatile     int             state    = IDLE;
    private volatile     boolean         shutdown = false;

    @Override
    public void run()
    {
        try
        {
            do
            {
                Runnable avail = queue.poll();
                if (avail != null)
                {
                    avail.run();
                }
                else
                {
                    state = IDLE;
                    if (queue.isEmpty() == false)
                    {
                        state = WORK;
                    }
                    else
                    {
                        LockSupport.park(this);
                        if (Thread.currentThread().isInterrupted() && shutdown)
                        {
                            break;
                        }
                    }
                }
            } while (true);
        }
        catch (Throwable e)
        {
            //代码不应该走到这里
            System.exit(-1);
        }
    }

    @Override
    public void submit(Runnable runnable)
    {
        queue.offer(runnable);
        int t_state = this.state;
        if (t_state == IDLE)
        {
            this.state = WORK;
            LockSupport.unpark(this);
        }
    }

    @Override
    public void shuwdown()
    {
        shutdown = true;
        interrupt();
    }

    @Override
    public Thread thread()
    {
        return this;
    }
}
