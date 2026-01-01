package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import lombok.Data;

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
        if (counter.get() < limit)
        {
            adaptiveReadCompletionHandler.registerRead();
        }
        else
        {
            set(IDLE);
            if (counter.get() < limit)
            {
                if (compareAndSet(IDLE, WORK))
                {
                    adaptiveReadCompletionHandler.registerRead();
                }
                else
                {
                    ;
                }
            }
        }
    }

    public void notifyRead()
    {
        int state = get();
        if (state == IDLE && counter.compareAndSet(IDLE, WORK))
        {
            adaptiveReadCompletionHandler.registerRead();
        }
    }
}
