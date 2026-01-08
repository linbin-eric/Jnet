package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NoticeWriteLimiter implements WriteListener
{
    private final AtomicInteger     counter;
    private final NoticeReadLimiter noticeReadLimiter;
    private final int               limit;

    @Override
    public void partWriteFinish(int currentSend)
    {
        int left = counter.addAndGet(0 - currentSend);
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
