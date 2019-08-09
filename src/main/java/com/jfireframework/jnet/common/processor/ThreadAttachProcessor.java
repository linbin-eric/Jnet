package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import org.jctools.queues.SpscLinkedQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadAttachProcessor extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    private              ExecutorService executorService;
    private static final int             IDLE = 0;
    private static final int             WORK = 1;

    public ThreadAttachProcessor(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    private ThreadLocal<Worker> local = new ThreadLocal<Worker>()
    {
        @Override
        protected Worker initialValue()
        {
            return new Worker();
        }
    };

    @Override
    public void process(IoBuffer data) throws Throwable
    {
        Worker worker = local.get();
        worker.offer(data);
        if (worker.get() == IDLE && worker.compareAndSet(IDLE, WORK))
        {
            executorService.submit(worker);
        }
    }

    class Worker extends AtomicInteger implements Runnable
    {
        private SpscLinkedQueue queue = new SpscLinkedQueue();

        public Worker()
        {
            super(IDLE);
        }

        public void offer(IoBuffer ioBuffer)
        {
            queue.offer(ioBuffer);
        }

        @Override
        public void run()
        {
            while (true)
            {
                IoBuffer tmp;
                while ((tmp = (IoBuffer) queue.poll()) != null)
                {
                    try
                    {
                        downStream.process(tmp);
                    }
                    catch (Throwable throwable)
                    {
                        ;
                    }
                }
                set(IDLE);
                if (queue.isEmpty() == false)
                {
                    if (compareAndSet(IDLE, WORK))
                    {
                        ;
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
        }
    }
}
