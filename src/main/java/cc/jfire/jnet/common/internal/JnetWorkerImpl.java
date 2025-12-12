package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.JnetWorker;
import org.jctools.queues.MpscLinkedQueue;

import java.util.concurrent.locks.LockSupport;

/**
 * 注意，一个JnetWorker会被分配给不同的通道，这意味着有多个生产者
 */
public class JnetWorkerImpl implements JnetWorker, Runnable
{
    private static final int                       IDLE     = -1;
    private static final int                       WORK     = 1;
    private final        MpscLinkedQueue<Runnable> queue    = new MpscLinkedQueue<>();
    private volatile     int                       state    = IDLE;
    private volatile     boolean                   shutdown = false;
    private final        Thread                    thread;

    public JnetWorkerImpl(Thread.UncaughtExceptionHandler uncaughtExceptionHandler)
    {
        thread = Thread.ofVirtual().uncaughtExceptionHandler(uncaughtExceptionHandler).unstarted(this);
        thread.start();
    }

    @Override
    public void run()
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
                if (!queue.isEmpty())
                {
                    state = WORK;
                }
                else
                {
                    LockSupport.park(thread);
                    if (Thread.currentThread().isInterrupted() && shutdown)
                    {
                        break;
                    }
                }
            }
        } while (true);
    }

    @Override
    public void submit(Runnable runnable)
    {
        queue.offer(runnable);
        int t_state = this.state;
        if (t_state == IDLE)
        {
            this.state = WORK;
            LockSupport.unpark(thread);
        }
    }

    @Override
    public void shutdown()
    {
        shutdown = true;
    }
}
