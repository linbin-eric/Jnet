package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;
import cc.jfire.jnet.common.internal.NoticeReadLimiter;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NoticeWriteLimiter implements WriteListener
{
    private final AtomicInteger     counter;
    private final NoticeReadLimiter noticeReadLimiter;
    private final int               limit;

    @Override
    public void partWriteFinish(long currentSend)
    {
        int left = counter.addAndGet((int) (0 - currentSend));
        if (left < limit)
        {
            noticeReadLimiter.notifyRead();
        }
    }

    @Override
    public void queuedWrite(long size)
    {
        counter.addAndGet((int) size);
    }

    @Override
    public void writeFailed(Throwable e)
    {
        counter.set(0);
    }
}
