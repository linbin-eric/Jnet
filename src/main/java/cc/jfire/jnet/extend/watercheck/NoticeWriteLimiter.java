package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;

import java.util.concurrent.atomic.AtomicInteger;

public record NoticeWriteLimiter(AtomicInteger counter, NoticeReadLimiter noticeReadLimiter, int limit) implements WriteListener
{
    @Override
    public void partWriteFinish(int currentSend)
    {
        int left = counter.addAndGet(-currentSend);
        if (left < limit)
        {
            noticeReadLimiter.notifyRead();
        }
    }

    @Override
    public void queuedWrite(int size)
    {
        counter.addAndGet(size);
    }

    @Override
    public void writeFailed(Throwable e)
    {
        counter.set(0);
    }
}
