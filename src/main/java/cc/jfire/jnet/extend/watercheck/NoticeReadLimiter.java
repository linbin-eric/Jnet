package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.internal.AdaptiveReadCompletionHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class NoticeReadLimiter extends AtomicInteger implements ReadProcessor<Void>
{
    private static final int                           WORK = 1;
    private static final int                           IDLE = 0;
    private final        AtomicInteger                 counter;
    private final        AdaptiveReadCompletionHandler adaptiveReadCompletionHandler;
    private final        int                           limit;

    public NoticeReadLimiter(AtomicInteger counter, AdaptiveReadCompletionHandler adaptiveReadCompletionHandler, int limit)
    {
        this.counter                       = counter;
        this.adaptiveReadCompletionHandler = adaptiveReadCompletionHandler;
        this.limit                         = limit;
        set(WORK);
    }

    @Override
    public void read(Void data, ReadProcessorNode next)
    {
        set(WORK);
        if (counter.get() < limit)
        {
            adaptiveReadCompletionHandler.registerRead();
        }
        else
        {
            setIdle();
            if (counter.get() < limit)
            {
                if (compareAndSet(IDLE, WORK))
                {
                    adaptiveReadCompletionHandler.registerRead();
                }
            }
        }
    }

    @Override
    public void readCompleted(ReadProcessorNode next)
    {
        if (counter.get() < limit)
        {
            adaptiveReadCompletionHandler.registerRead();
        }
        else
        {
            setIdle();
            if (counter.get() < limit)
            {
                if (compareAndExchange(IDLE, WORK) == IDLE)
                {
                    adaptiveReadCompletionHandler.registerRead();
                }
            }
        }
    }

    private void setIdle()
    {
        set(IDLE);
    }

    public void notifyRead()
    {
        if (compareAndExchange(IDLE, WORK) == IDLE)
        {
            adaptiveReadCompletionHandler.registerRead();
        }
    }
}
