package com.jfireframework.jnet.common.processor;

import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.recycler.RecycleHandler;
import com.jfireframework.jnet.common.recycler.Recycler;
import org.jctools.queues.SpscLinkedQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ThreadAttachProcessor extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    private              ExecutorService executorService;
    private static final int             IDLE = 0;
    private static final int             WORK = 1;

    public ThreadAttachProcessor(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    private static final Recycler<Entry>     RECYCLER = new Recycler<Entry>()
    {
        @Override
        protected Entry newObject(RecycleHandler handler)
        {
            Entry entry = new Entry();
            entry.handler = handler;
            return entry;
        }
    };
    private static       ThreadLocal<Worker> local    = new ThreadLocal<Worker>();
    int num = 0;

    @Override
    public void process(IoBuffer data) throws Throwable
    {
        Worker worker = local.get();
        if (worker == null)
        {
            worker = new Worker();
            local.set(worker);
            executorService.submit(worker);
        }
        Entry entry = RECYCLER.get();
        entry.downStream = downStream;
        entry.data = data;
        worker.offer(entry);
        num++;
        if (worker.get() == IDLE)
        {
            worker.awake();
        }
    }

    class Worker extends AtomicInteger implements Runnable
    {
        private          SpscLinkedQueue queue = new SpscLinkedQueue();
        private volatile Thread          owner;

        public Worker()
        {
            super(IDLE);
        }

        public void offer(Object data)
        {
            queue.offer(data);
        }

        public void awake()
        {
            LockSupport.unpark(owner);
        }

        int count = 0;

        @Override
        public void run()
        {
            owner = Thread.currentThread();
            while (true)
            {
                Entry tmp;
                set(WORK);
                while ((tmp = (Entry) queue.poll()) != null)
                {
                    try
                    {
                        tmp.downStream.process(tmp.data);
                        RecycleHandler handler = tmp.handler;
                        tmp.data = null;
                        tmp.downStream = null;
                        handler.recycle(tmp);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }
                    count++;
                }
                set(IDLE);
                if (queue.isEmpty() == false)
                {
                    continue;
                }
                else
                {
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
                }
            }
        }
    }

    static class Entry
    {
        IoBuffer       data;
        DataProcessor  downStream;
        RecycleHandler handler;
    }
}
