package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadListener;
import cc.jfire.jnet.common.internal.AdaptiveReadCompletionHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractReadLimiter extends AtomicInteger implements ReadListener
{
    private static final int                           work = 1;
    private static final int                           idle = 0;
    protected            AdaptiveReadCompletionHandler adaptiveReadCompletionHandler;
    protected final      long                          LIMIT;

    public AbstractReadLimiter(long limit)
    {
        super(work);
        this.LIMIT = limit;
    }

    @Override
    public void onNeedRegister(AdaptiveReadCompletionHandler readCompletionHandler, Pipeline pipeline)
    {
        long capacity = getLimitSize();
        if (capacity == -1 || capacity < LIMIT)
        {
            readCompletionHandler.registerRead();
        }
        else
        {
            setIdle(readCompletionHandler);
        }
    }

    public void tryRegisterRead()
    {
        int now = get();
        if (now == idle && compareAndSet(idle, work))
        {
            adaptiveReadCompletionHandler.registerRead();
        }
    }

    public void setIdle(AdaptiveReadCompletionHandler readCompletionHandler)
    {
        adaptiveReadCompletionHandler = readCompletionHandler;
        set(idle);
        if (getLimitSize() < LIMIT && compareAndSet(idle, work))
        {
            readCompletionHandler.registerRead();
        }
        else
        {
        }
    }

    public abstract long getLimitSize();
}
